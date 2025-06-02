from flask import Flask, jsonify, request
import pickle
import numpy as np
from flask_cors import CORS
import os
import random
import requests  # For making API calls to Spring Boot
import json
from datetime import datetime
import logging

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Initialize the Flask app
app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Configuration
SPRING_BOOT_API = os.environ.get('SPRING_BOOT_API', 'http://localhost:8088/api/v1')

# Try to load the model and dataset if they exist
model = None
dataset = None

try:
    with open("lightfm_model.pkl", "rb") as f:
        model = pickle.load(f)
    with open("lightfm_dataset.pkl", "rb") as f:
        dataset = pickle.load(f)
    logger.info("Successfully loaded model and dataset")
except FileNotFoundError:
    logger.warning("Model or dataset files not found. Will use dummy data instead.")
except Exception as e:
    logger.error(f"Error loading model: {e}")

@app.route("/recommend/<int:user_id>")
def recommend(user_id):
    """Get book recommendations for a specific user"""
    try:
        # Get number of recommendations from query param, default to 5
        num_recommendations = request.args.get('count', default=5, type=int)
        
        # Determine if we should blend in recent top-rated books
        blend_recent = request.args.get('blend_recent', default='true', type=str).lower() == 'true'
        
        # Check for a force_include parameter (book ID to always include)
        force_include = request.args.get('force_include', default=None, type=int)
        
        logger.info(f"Recommendation request for user {user_id}, count: {num_recommendations}, blend_recent: {blend_recent}, force_include: {force_include}")
        
        # Get model-based recommendations
        if model is None or dataset is None:
            logger.info("Using dummy recommendations (no model available)")
            model_recommendations = get_dummy_recommendations(count=num_recommendations)
        else:
            try:
                user_map = dataset.mapping()[0]
                item_map = dataset.mapping()[2]
                item_id_reverse = {v: k for k, v in item_map.items()}
                n_items = model.item_embeddings.shape[0]
                
                if user_id not in user_map:
                    logger.info(f"User {user_id} not found, returning popular recommendations")
                    model_recommendations = get_dummy_recommendations(count=num_recommendations)
                else:
                    user_x = user_map[user_id]
                    scores = model.predict(user_x, np.arange(n_items))
                    top_items = np.argsort(-scores)[:num_recommendations]
                    
                    model_recommendations = [
                        {"book_id": int(item_id_reverse[i]), "score": float(scores[i])}
                        for i in top_items
                    ]
            except Exception as e:
                logger.error(f"Error generating model recommendations: {e}")
                model_recommendations = get_dummy_recommendations(count=num_recommendations)
        
        # If blending is enabled, get and incorporate recent top-rated books
        if blend_recent:
            try:
                # Get number of days and recent books
                days = request.args.get('days', default=30, type=int)
                recent_count = request.args.get('recent_count', default=3, type=int)
                
                # Fetch recent top-rated books from Spring Boot API
                logger.info(f"Fetching recent top-rated books: days={days}, count={recent_count}")
                recent_books = get_recent_top_rated_books(days, recent_count)
                
                # Blend the recommendations
                final_recommendations = blend_recommendations(
                    model_recommendations, 
                    recent_books, 
                    num_recommendations
                )
            except Exception as e:
                logger.error(f"Error blending recommendations: {e}")
                final_recommendations = model_recommendations
        else:
            final_recommendations = model_recommendations
            
        # If force_include is specified, make sure it's in the recommendations
        if force_include is not None:
            # Check if already in recommendations
            book_ids = [rec["book_id"] for rec in final_recommendations]
            if force_include not in book_ids:
                logger.info(f"Force including book ID {force_include} in recommendations")
                # Add it at the beginning with a high score
                final_recommendations.insert(0, {
                    "book_id": force_include,
                    "score": 0.99,
                    "is_forced": True
                })
                # Ensure we don't exceed requested count
                if len(final_recommendations) > num_recommendations:
                    final_recommendations = final_recommendations[:num_recommendations]
                    
        logger.info(f"Returning {len(final_recommendations)} recommendations: {[r['book_id'] for r in final_recommendations]}")
        return jsonify(final_recommendations)
    except Exception as e:
        logger.error(f"Error generating recommendations: {e}")
        return jsonify(get_dummy_recommendations(count=num_recommendations))

@app.route("/update_book", methods=['POST'])
def update_book():
    """Update the recommendation dataset with a new book"""
    book_data = request.json
    
    logger.info(f"Received book update request: {book_data}")
    
    if not book_data or 'book_id' not in book_data:
        logger.warning("Invalid book data received")
        return jsonify({"error": "Invalid book data"}), 400
    
    try:
        # If we don't have a real model, just return success
        if model is None or dataset is None:
            logger.info(f"No model loaded, acknowledging update for book_id: {book_data['book_id']}")
            return jsonify({"success": True, "message": "Using dummy model, no update needed"})
        
        book_id = book_data['book_id']
        
        # Check if book already exists in dataset
        item_map = dataset.mapping()[2]
        if book_id in item_map:
            logger.info(f"Book {book_id} already exists in dataset")
            return jsonify({"success": True, "message": f"Book {book_id} already in dataset"})
            
        # Add book to dataset
        # Note: This is a simplified approach. In a real system, you would:
        # 1. Update the interactions matrix
        # 2. Add new book features
        # 3. Retrain the model or use incremental updates
        
        # For this example, we'll just acknowledge the update
        # In a production system, you would queue this for model retraining
        
        logger.info(f"Book {book_id} would be added to dataset in a production system")
        
        return jsonify({
            "success": True, 
            "message": f"Book {book_id} acknowledged for future model update"
        })
        
    except Exception as e:
        logger.error(f"Error updating recommendation dataset: {e}")
        return jsonify({"error": str(e)}), 500

def get_recent_top_rated_books(days=30, count=3):
    """Fetch recent highly rated books from the Spring Boot API"""
    try:
        # If in development/testing mode, use dummy data
        if model is None or dataset is None:
            logger.info("Using dummy recent books (no model available)")
            return get_dummy_recent_books(count)
            
        # In production, call the Spring Boot API
        url = f"{SPRING_BOOT_API}/books/recent-top-rated?days={days}&count={count}"
        logger.info(f"Calling Spring Boot API: {url}")
        response = requests.get(url)
        
        if response.status_code == 200:
            books = response.json()
            logger.info(f"Received {len(books)} recent books from Spring Boot API")
            # Transform the response to match our recommendation format
            result = [{"book_id": book.get('id'), "score": 0.99, "is_recent": True} for book in books]
            logger.info(f"Transformed books: {result}")
            return result
        else:
            logger.error(f"Error fetching recent books: {response.status_code}")
            return get_dummy_recent_books(count)
    except Exception as e:
        logger.error(f"Error fetching recent top rated books: {e}")
        return get_dummy_recent_books(count)

def get_dummy_recent_books(count=3):
    """Generate dummy recent books for testing"""
    # Use actual existing book IDs from your database
    books = [1, 2, 3, 4, 5]  # Replace these with real book IDs from your database
    selected_books = random.sample(books, min(count, len(books)))
    
    recent_books = []
    for i, book_id in enumerate(selected_books):
        score = 0.99  # High score for recent highly-rated books
        recent_books.append({"book_id": book_id, "score": score, "is_recent": True})
    
    logger.info(f"Generated {len(recent_books)} dummy recent books: {[r['book_id'] for r in recent_books]}")
    return recent_books

def blend_recommendations(model_recs, recent_recs, total_count):
    """Blend model recommendations with recent top-rated books"""
    # Remove any duplicates (if a book is both recommended by model and is recent)
    model_book_ids = set(rec["book_id"] for rec in model_recs)
    unique_recent_recs = [rec for rec in recent_recs if rec["book_id"] not in model_book_ids]
    
    logger.info(f"Blending {len(model_recs)} model recommendations and {len(unique_recent_recs)} unique recent books")
    
    # Determine how many recent books to include
    # A good strategy is to include at least 1 recent book, but not more than 30% of total
    num_recent = min(len(unique_recent_recs), max(1, total_count // 3))
    
    # Take top model recommendations and top recent recommendations
    final_recs = model_recs[:total_count - num_recent] + unique_recent_recs[:num_recent]
    
    # Sort by score for consistent ordering
    final_recs.sort(key=lambda x: x["score"], reverse=True)
    
    logger.info(f"Blended recommendations: {[r['book_id'] for r in final_recs]}")
    return final_recs

@app.route("/users")
def list_users():
    """List all users available in the dataset"""
    try:
        if model is None or dataset is None:
            dummy_users = [1, 2, 3, 4, 5]
            logger.info(f"Returning dummy users: {dummy_users}")
            return jsonify(dummy_users)
            
        user_map = dataset.mapping()[0]
        users = sorted(user_map.keys())
        logger.info(f"Returning {len(users)} users from dataset")
        return jsonify(users)
    except Exception as e:
        logger.error(f"Error listing users: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/books")
def list_books():
    """List all books available in the dataset"""
    try:
        if model is None or dataset is None:
            dummy_books = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
            logger.info(f"Returning dummy books: {dummy_books}")
            return jsonify(dummy_books)
            
        item_map = dataset.mapping()[2]
        books = sorted(item_map.keys())
        logger.info(f"Returning {len(books)} books from dataset")
        return jsonify(books)
    except Exception as e:
        logger.error(f"Error listing books: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/recent-books", methods=['GET'])
def get_recent_books():
    """Get recent books to blend into recommendations"""
    days = request.args.get('days', default=30, type=int)
    count = request.args.get('count', default=3, type=int)
    logger.info(f"Recent books request: days={days}, count={count}")
    books = get_recent_top_rated_books(days, count)
    return jsonify(books)

@app.route("/health")
def health_check():
    """Simple health check endpoint"""
    logger.info("Health check requested")
    return jsonify({"status": "healthy", "timestamp": datetime.now().isoformat()})

def get_dummy_recommendations(count=5):
    """Generate dummy recommendation data for testing"""
    # Use realistic book IDs that match your Spring Boot database
    books = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    selected_books = random.sample(books, min(count, len(books)))
    
    recommendations = []
    for i, book_id in enumerate(selected_books):
        score = round(0.95 - (i * 0.05), 2)  # Scores from 0.95 down to 0.75
        recommendations.append({"book_id": book_id, "score": score})
    
    logger.info(f"Generated {len(recommendations)} dummy recommendations: {[r['book_id'] for r in recommendations]}")
    return recommendations

@app.route("/force-recommend/<int:book_id>")
def force_recommend(book_id):
    """Force a specific book to be recommended"""
    logger.info(f"Force recommending book ID: {book_id}")
    recommendations = get_dummy_recommendations(4)  # Get 4 random books
    
    # Add the forced book at the beginning
    forced_rec = {"book_id": book_id, "score": 0.99, "is_forced": True}
    recommendations.insert(0, forced_rec)
    
    return jsonify(recommendations)

if __name__ == "__main__":
    # Use the PORT environment variable if available
    port = int(os.environ.get('PORT', 5000))
    logger.info(f"Starting Flask recommendation service on port {port}")
    app.run(debug=True, host='0.0.0.0', port=port)
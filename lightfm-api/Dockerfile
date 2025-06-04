# Use an official Python base image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install build tools required for lightfm
RUN apt-get update && apt-get install -y \
    build-essential \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy files into container
COPY . .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Expose Flask's port
EXPOSE 5000

# Run the app
CMD ["python", "app.py"]

package com.alibou.book.vectorstore;

import com.alibou.book.book.Book;
import com.alibou.book.book.BookRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class VectorStoreCommandLineRunner implements CommandLineRunner {
    private final VectorStore vectorStore;
    private final BookRepository bookRepository;

    public VectorStoreCommandLineRunner(VectorStore vectorStore, BookRepository bookRepository) {
        this.vectorStore = vectorStore;
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Book> books = this.bookRepository.findAll();
        List<Document> documents = books
                .stream()
                .map(this::toDocument)
                .peek(doc -> System.out.println("Adding to vector store: " + doc.getFormattedContent()))
                .toList();
        this.vectorStore.add(documents);
    }

    private Document toDocument(Book book) {
        String bookContent = """
            Title: %s
            Author: %s
            Synopsis: %s
            ISBN: %s
            Rating: %.1f
            """.formatted(
                book.getTitle(),
                book.getAuthorName(),
                book.getSynopsis() != null ? book.getSynopsis() : "",
                book.getIsbn(),
                book.getRate()
        );

        Map<String, Object> metadata = Map.of(
                "bookId", book.getId(),
                "isbn", book.getIsbn(),
                "archived", book.isArchived(),
                "shareable", book.isShareable(),
                "rating", book.getRate()
        );

        return new Document(bookContent, metadata);
    }
}

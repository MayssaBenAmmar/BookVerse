package com.alibou.book.event;

import com.alibou.book.book.Book;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookEvent extends ApplicationEvent {
    private final Book book;
    private final BookEventType eventType;

    public BookEvent(Object source, Book book, BookEventType eventType) {
        super(source);
        this.book = book;
        this.eventType = eventType;
    }

    public enum BookEventType {
        CREATED,
        UPDATED,
        DELETED,
        SHARED,
        UNSHARED,
        ARCHIVED,
        UNARCHIVED
    }
}
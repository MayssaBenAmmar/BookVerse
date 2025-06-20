package com.alibou.book.history;

import com.alibou.book.book.Book;
import com.alibou.book.common.BaseEntity;
import com.alibou.book.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BookTransactionHistory extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id") // Make sure 'id' matches User primary key
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    private boolean returned;
    private boolean returnApproved;

    // NEW: Added date fields for tracking
    @Column(name = "borrowed_date")
    private LocalDateTime borrowedDate;

    @Column(name = "returned_date")
    private LocalDateTime returnedDate;
}
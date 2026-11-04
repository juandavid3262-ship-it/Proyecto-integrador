package dev.juanmateus.bibliotech.repository;

import dev.juanmateus.bibliotech.model.constant.BookFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import dev.juanmateus.bibliotech.model.entity.Book;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    public List<Book> findByTitleContainingIgnoreCase(String title);

    public List<Book> findByAuthor(String author);

    public List<Book> findByGenreName(String genre);

    public List<Book> findByFormat(BookFormat format);
}


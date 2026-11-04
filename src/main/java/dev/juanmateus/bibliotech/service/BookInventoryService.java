package dev.juanmateus.bibliotech.service;

import dev.juanmateus.bibliotech.model.constant.BookFormat;
import dev.juanmateus.bibliotech.model.dto.BookDTO;
import dev.juanmateus.bibliotech.model.entity.Book;
import dev.juanmateus.bibliotech.model.entity.Genre;
import dev.juanmateus.bibliotech.repository.BookRepository;
import dev.juanmateus.bibliotech.repository.GenreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class BookInventoryService {
    private final BookRepository repository;
    private final GenreRepository genreRepository;

    public BookInventoryService(BookRepository repository, GenreRepository genreRepository) {
        this.repository = repository;
        this.genreRepository = genreRepository;
    }

    public List<Book> findAllBooks() {
        return this.repository.findAll();
    }

    public List<Book> findBooksByTitle(String title) {
        return this.repository.findByTitleContainingIgnoreCase(title);
    }

    public List<Book> findBooksByAuthor(String author) {
        return this.repository.findByAuthor(author);
    }

    public List<Book> findBooksByGenre(String genre) {
        return this.repository.findByGenreName(genre);
    }

    public List<Book> findBooksByFormat(BookFormat format) {
        return this.repository.findByFormat(format);
    }

    public BookDTO createBook(BookDTO bookRequest) throws EntityNotFoundException, IllegalArgumentException {
        if (!isvalidateISBN(bookRequest.isbn())){
            throw new IllegalArgumentException("ISBN no valido");
        }
        // busca el genero mencionado en el dto
        Genre genre = findGenre(bookRequest.genre());

        Book book = new Book();

        setAtributesFromDTO(bookRequest, book, genre);

        this.repository.save(book);

        return bookRequest;
    }

    public BookDTO updateBook(Long bookId, BookDTO bookRequest) throws EntityNotFoundException {
        Book book = this.repository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Libro con Id " + bookId + "no encontrado"));

        Genre genre = findGenre(bookRequest.genre());

        setAtributesFromDTO(bookRequest, book, genre);

        this.repository.save(book);

        return bookRequest;
    }

    public void deleteBook(Long bookId) throws EntityNotFoundException {
        Book book = this.repository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Libro con id " + bookId + "no encontrado"));

        this.repository.delete(book);
    }

    public void setAtributesFromDTO(BookDTO request, Book book, Genre genre) {
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setGenre(genre);
        book.setFormat(request.format());
        book.setQuantity(request.quantity());
    }

    public Genre findGenre(String name) {
        return this.genreRepository
                .findByNameIgnoreCase(name)
                .orElseThrow(() -> new EntityNotFoundException("Genero de nombre " + name + "no encontrado"));
    }

    private boolean isvalidateISBN(String isbn) {
        // el isbn es de 10 caracteres?
        if (isbn.length() != 10) {
            return false;
        }
        int sum = 0;

        for (int index = 0; index < 9; index++) {
            char caracter = isbn.charAt(index);
            // el caracter es numero?
            if (!Character.isDigit(caracter)) {
                return false;
            }

            int digit = Character.getNumericValue(caracter);
            if (digit < 0 || digit > 9) {
                return false;
            }

            int weight = 10 - index;
            sum += digit * weight;
        }
        return false;
    }
    }












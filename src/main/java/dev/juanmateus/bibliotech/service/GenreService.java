package dev.juanmateus.bibliotech.service;

import dev.juanmateus.bibliotech.model.entity.Genre;
import dev.juanmateus.bibliotech.repository.GenreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GenreService {
    private final GenreRepository repository;

    public GenreService(GenreRepository repository) {
        this.repository = repository;
    }

    public List<Genre> findAllGenres() {
        return this.repository.findAll();
    }

    public String createGenre(String genreName) {
        Genre genre = new Genre(); //CREA NUEVO OBJETO
        genre.setName(genreName); //LE PONE AL OBJETO EL NOMBRE QUE RECIBE

        repository.save(genre); //GUARDA EL GENERO EN LA BASE DE DATOS
        return genreName; //DEVUELVE EL NOMBRE DEL GENERO CREADO
    }

    public String updateGenre(Long genreId, String genreName) throws EntityNotFoundException {
        Genre genre = this.repository.findById(genreId)
                .orElseThrow(() -> new EntityNotFoundException("Género con id " + genreId + " no encontrado"));

        genre.setName(genreName);
        this.repository.save(genre);
        return genreName;
    }

    public void deleteGenre(Long genreId) throws EntityNotFoundException {
        Genre genre = this.repository.findById(genreId)
                .orElseThrow(() -> new EntityNotFoundException("Género con id " + genreId + " no encontrado"));

        this.repository.delete(genre);
    }

    public Optional<Genre> findGenreById(Long genreId) {
        return repository.findById(genreId);
    }
}
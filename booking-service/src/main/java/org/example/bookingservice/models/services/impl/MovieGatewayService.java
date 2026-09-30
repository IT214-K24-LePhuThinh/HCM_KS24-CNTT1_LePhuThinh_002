package org.example.bookingservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.bookingservice.clients.MovieClient;
import org.example.bookingservice.exceptions.MovieNotFoundException;
import org.example.bookingservice.exceptions.MovieServiceException;
import org.example.bookingservice.models.dto.responses.MovieResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieGatewayService {

    private final MovieClient movieClient;

    @CircuitBreaker(name = "movieService", fallbackMethod = "getMovieByFallback")
    public MovieResponse getMovieById(Long movieId) {
        try {
            MovieResponse movie = movieClient.getMovieById(movieId);
            if (movie == null) throw new MovieNotFoundException(movieId);

            return movie;
        } catch (FeignException.NotFound e) {
            throw new MovieNotFoundException(movieId);
        } catch (FeignException e) {
            throw new MovieServiceException("Lỗi movie Service: " + e);
        }
    }

    private void getMovieByIdFallback(Long movieId, Throwable throwable) {
        if (throwable instanceof MovieNotFoundException e) {
            throw e;
        }

        throw new MovieServiceException("Movie Service không hoạt động");

    }

}

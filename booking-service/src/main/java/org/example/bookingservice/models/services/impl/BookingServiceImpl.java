package org.example.bookingservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookingservice.exceptions.MovieNotFoundException;
import org.example.bookingservice.models.constants.BookingStatus;
import org.example.bookingservice.models.dto.requests.CreateBookingDetailRequest;
import org.example.bookingservice.models.dto.requests.CreateBookingRequest;
import org.example.bookingservice.models.dto.responses.BookingDetailResponse;
import org.example.bookingservice.models.dto.responses.BookingResponse;
import org.example.bookingservice.models.dto.responses.MovieResponse;
import org.example.bookingservice.models.entities.Booking;
import org.example.bookingservice.models.entities.BookingDetail;
import org.example.bookingservice.models.repositories.BookingDetailRepository;
import org.example.bookingservice.models.repositories.BookingRepository;
import org.example.bookingservice.models.services.BookingService;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

        private final BookingRepository bookingRepository;
        private final BookingDetailRepository bookingDetailRepository;
        private final MovieGatewayService movieGatewayService;
        private final KafkaTemplate<String, String> kafkaTemplate;
        private final ApplicationEventPublisher eventPublisher;

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void publishBookingCreated(String customerEmail) {
                kafkaTemplate.send("booking-created", customerEmail);
        }

        @Override
        @Transactional
        public BookingResponse createBooking(CreateBookingRequest request) {
                Booking booking = bookingRepository.save(Booking.builder()
                        .customerName(request.customerName())
                        .customerEmail(request.customerEmail())
                        .total(0.0)
                        .status(BookingStatus.PENDING)
                        .build());

                double totalAmount = 0;
                List<BookingDetailResponse> items = new ArrayList<>();

                for (CreateBookingDetailRequest item: request.items()) {
                        MovieResponse movie = movieGatewayService.getMovieById(item.movieId());

                        if (movie == null) throw new MovieNotFoundException(item.movieId());

                        BookingDetail bookingDetail = bookingDetailRepository.save(BookingDetail.builder()
                                .booking(booking)
                                .movieId(item.movieId())
                                .quantity(item.quantity())
                                .unitPrice(movie.ticketPrice())
                                .build());

                        double price = item.quantity() * movie.ticketPrice();

                        items.add(new BookingDetailResponse(
                                bookingDetail.getId(), movie.id(), movie.title(), item.quantity(), movie.ticketPrice(), price
                        ));

                        totalAmount += price;
                }

                booking.setTotal(totalAmount);
                bookingRepository.save(booking);
                eventPublisher.publishEvent(request.customerEmail());

                return new BookingResponse(
                        booking.getId(), request.customerName(), request.customerEmail(), totalAmount, booking.getStatus(), items
                );
        }
}


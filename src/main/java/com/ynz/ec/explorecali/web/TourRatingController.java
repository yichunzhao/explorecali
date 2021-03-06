package com.ynz.ec.explorecali.web;

import com.ynz.ec.explorecali.domain.Tour;
import com.ynz.ec.explorecali.domain.TourRating;
import com.ynz.ec.explorecali.domain.TourRatingPK;
import com.ynz.ec.explorecali.repo.TourRatingRepository;
import com.ynz.ec.explorecali.repo.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.AbstractMap;
import java.util.List;
import java.util.OptionalDouble;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(value = "/tours/{tourId}/ratings")
public class TourRatingController {
    private TourRatingRepository tourRatingRepository;
    private TourRepository tourRepository;

    @Autowired
    public TourRatingController(TourRatingRepository tourRatingRepository, TourRepository tourRepository) {
        this.tourRatingRepository = tourRatingRepository;
        this.tourRepository = tourRepository;
    }

    //what difference?
    //@Validated: Spring annotation
    //@Valid: java annotation
    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public void createTourRating(@PathVariable("tourId") Integer tourId, @RequestBody @Validated RatingDto ratingDto) {
        Tour tour = verifyTourId(tourId);

        tourRatingRepository.save(new TourRating(new TourRatingPK(tour, ratingDto.getCustomerId()), ratingDto.getScore(), ratingDto.getComment()));
    }

    //Pageable: size(items in one page), page number, sorting by attribute(a or d sorting).
    //Fx: URL  http://localhost:8080/tours/1/ratings?size=3&page=1$sort=score,asc
    @GetMapping
    public List<RatingDto> getAllRatingsForTour(@PathVariable("tourId") Integer tourId, Pageable pageable) {
        Tour tour = verifyTourId(tourId);

        Page<TourRating> tourRatingPage = tourRatingRepository.findByRatingPKTourId(tourId, pageable);
        return tourRatingPage.getContent()
                .stream()
                .map(r -> new RatingDto(r.getScore(), r.getComment(), r.getRatingPK().getCustomerId()))
                .collect(toList());
    }

    @GetMapping("/average")
    public AbstractMap.SimpleEntry getAverageRatingForTour(@PathVariable("tourId") Integer tourId) {

        OptionalDouble average = tourRatingRepository.findByRatingPKTourId(tourId).stream().mapToInt(r -> r.getScore()).average();

        return new AbstractMap.SimpleEntry<String, Double>("average:", average.isPresent() ? average.getAsDouble() : null);
    }

    //PUT -Update all non-key attributes
    @PutMapping
    public RatingDto updateWithPUt(@PathVariable("tourId") Integer tourId, @RequestBody @Validated RatingDto ratingDto) {
        TourRating tourRating = verifyTourRating(tourId, ratingDto.getCustomerId());

        if (ratingDto.getScore() != null) tourRating.setScore(ratingDto.getScore());
        if (ratingDto.getComment() != null) tourRating.setComment(ratingDto.getComment());

        return toDto(tourRatingRepository.save(tourRating));
    }

    //Patch - update partial non-key attributes
    @PatchMapping
    public RatingDto updateWithPatch(@PathVariable("tourId") Integer tourId, @RequestBody @Validated RatingDto ratingDto) {
        TourRating tourRating = verifyTourRating(tourId, ratingDto.getCustomerId());

        tourRating.setScore(ratingDto.getScore());
        tourRating.setComment(ratingDto.getComment());

        return toDto(tourRatingRepository.save(tourRating));
    }

    //Delete
    @DeleteMapping(value = "/{customerId}")
    public void delete(@PathVariable("tourId") Integer tourId, @PathVariable("customerId") Integer customerId) {
        TourRating tourRating = verifyTourRating(tourId, customerId);
        tourRatingRepository.delete(tourRating);
    }

    /**
     * Convert domain model into DTO.
     *
     * @param tourRating instance.
     * @return its corresponding DTO instance.
     */
    private RatingDto toDto(TourRating tourRating) {
        return new RatingDto(tourRating.getScore(), tourRating.getComment(), tourRating.getRatingPK().getCustomerId());
    }

    /**
     * Verify and return TourRating for a specific TourRating PK(tourId, CustomerId)
     *
     * @param tourId     part of composite PK
     * @param customerId part of composite PK
     * @return specified {@link TourRating} instance.
     */
    private TourRating verifyTourRating(Integer tourId, Integer customerId) {
        return tourRatingRepository.findByRatingPKTourIdAndRatingPKCustomerId(tourId, customerId)
                .orElseThrow(() -> new TourRatingNotFoundException("The TourRating is not found."));
    }

    //Patch -update one or more non-key attributes
    private Tour verifyTourId(Integer tourId) {
        return tourRepository.findById(tourId).orElseThrow(() -> new TourNotFoundException("tourId is not found."));
    }

    //@ResponseStatus is used on the method level;
    @ExceptionHandler(TourNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public String returnNotFound(TourNotFoundException e) {
        return e.getMessage();
    }

    @ExceptionHandler(TourRatingNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public String returnTourRatingNotFound(TourRatingNotFoundException e) {
        return e.getMessage();
    }


}

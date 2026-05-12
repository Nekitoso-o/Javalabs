package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.ComicImageDto;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.ComicImage;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.repository.ComicImageRepository;
import com.example.mangacatalog.repository.ComicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ComicImageService {

    private static final String COVERS_DIR = "covers";
    private static final String COMIC_NOT_FOUND = "Комикс с ID %s не найден!";
    private static final String IMAGE_NOT_FOUND = "Изображение с ID %s не найдено!";

    @Value("${app.base-url:}")
    private String baseUrl;

    private final ComicImageRepository imageRepository;
    private final ComicRepository comicRepository;
    private final FileStorageService fileStorage;

    public ComicImageService(ComicImageRepository imageRepository,
                             ComicRepository comicRepository,
                             FileStorageService fileStorage) {
        this.imageRepository = imageRepository;
        this.comicRepository = comicRepository;
        this.fileStorage = fileStorage;
    }

    public List<ComicImageDto> getImages(Long comicId) {
        return imageRepository.findByComicIdOrderBySortOrderAsc(comicId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public List<ComicImageDto> uploadImages(Long comicId,
                                            MultipartFile[] files) throws IOException {
        Comic comic = comicRepository.findById(comicId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(COMIC_NOT_FOUND, comicId)));

        List<ComicImage> existing = imageRepository
            .findByComicIdOrderBySortOrderAsc(comicId);
        int nextOrder = existing.size();

        for (MultipartFile file : files) {
            String fileName = fileStorage.store(file, COVERS_DIR);

            ComicImage image = new ComicImage();
            image.setComic(comic);
            image.setFileName(fileName);
            image.setOriginalName(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : fileName);
            image.setContentType(file.getContentType());
            image.setSortOrder(nextOrder++);
            imageRepository.save(image);
        }

        return getImages(comicId);
    }

    @Transactional
    public void deleteImage(Long comicId, Long imageId) {
        ComicImage image = imageRepository.findById(imageId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(IMAGE_NOT_FOUND, imageId)));

        if (!image.getComic().getId().equals(comicId)) {
            throw new ResourceNotFoundException(
                String.format(IMAGE_NOT_FOUND, imageId));
        }

        fileStorage.delete(COVERS_DIR, image.getFileName());
        imageRepository.delete(image);
    }

    @Transactional
    public List<ComicImageDto> reorder(Long comicId, List<Long> orderedIds) {
        List<ComicImage> images = imageRepository
            .findByComicIdOrderBySortOrderAsc(comicId);

        for (int i = 0; i < orderedIds.size(); i++) {
            final int order = i;
            final Long imgId = orderedIds.get(i);
            images.stream()
                .filter(img -> img.getId().equals(imgId))
                .findFirst()
                .ifPresent(img -> {
                    img.setSortOrder(order);
                    imageRepository.save(img);
                });
        }
        return getImages(comicId);
    }

    public ComicImageDto toDto(ComicImage img) {
        String url = baseUrl + "/api/images/covers/" + img.getFileName();
        return new ComicImageDto(
            img.getId(), url,
            img.getOriginalName(),
            img.getSortOrder()
        );
    }
}
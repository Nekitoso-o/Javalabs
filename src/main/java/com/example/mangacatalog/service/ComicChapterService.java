package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.ChapterPageDto;
import com.example.mangacatalog.dto.ComicChapterDto;
import com.example.mangacatalog.entity.ChapterPage;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.ComicChapter;
import com.example.mangacatalog.exception.ResourceNotFoundException;
import com.example.mangacatalog.repository.ComicChapterRepository;
import com.example.mangacatalog.repository.ComicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ComicChapterService {

    private static final String CHAPTERS_DIR = "chapters";
    private static final String COMIC_NOT_FOUND = "Комикс с ID %s не найден!";
    private static final String CHAPTER_NOT_FOUND = "Глава с ID %s не найдена!";

    private final ComicChapterRepository chapterRepository;
    private final ComicRepository comicRepository;
    private final FileStorageService fileStorage;

    public ComicChapterService(ComicChapterRepository chapterRepository,
                               ComicRepository comicRepository,
                               FileStorageService fileStorage) {
        this.chapterRepository = chapterRepository;
        this.comicRepository = comicRepository;
        this.fileStorage = fileStorage;
    }

    public List<ComicChapterDto> getChapters(Long comicId) {
        return chapterRepository.findByComicIdOrdered(comicId)
            .stream()
            .map(c -> toDto(c, false))
            .toList();
    }

    public ComicChapterDto getChapter(Long comicId, Long chapterId) {
        ComicChapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(CHAPTER_NOT_FOUND, chapterId)));

        if (!chapter.getComic().getId().equals(comicId)) {
            throw new ResourceNotFoundException(
                String.format(CHAPTER_NOT_FOUND, chapterId));
        }
        return toDto(chapter, true);
    }

    @Transactional
    public ComicChapterDto createChapter(Long comicId,
                                         Double chapterNumber,
                                         String title,
                                         MultipartFile[] files) throws IOException {
        Comic comic = comicRepository.findById(comicId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(COMIC_NOT_FOUND, comicId)));

        if (chapterRepository.existsByComicIdAndChapterNumber(comicId, chapterNumber)) {
            throw new IllegalArgumentException(
                "Глава " + chapterNumber + " уже существует для этого комикса");
        }

        ComicChapter chapter = new ComicChapter();
        chapter.setComic(comic);
        chapter.setChapterNumber(chapterNumber);
        chapter.setTitle(title);

        List<ChapterPage> pages = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file.isEmpty()) continue;

            String fileName = fileStorage.store(file, CHAPTERS_DIR);
            ChapterPage page = new ChapterPage();
            page.setChapter(chapter);
            page.setFileName(fileName);
            page.setOriginalName(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : fileName);
            page.setContentType(file.getContentType());
            page.setSortOrder(i);
            pages.add(page);
        }

        chapter.setPages(pages);
        ComicChapter saved = chapterRepository.save(chapter);
        return toDto(saved, true);
    }


    @Transactional
    public void deleteChapter(Long comicId, Long chapterId) {
        ComicChapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(CHAPTER_NOT_FOUND, chapterId)));

        if (!chapter.getComic().getId().equals(comicId)) {
            throw new ResourceNotFoundException(
                String.format(CHAPTER_NOT_FOUND, chapterId));
        }

        // Удаляем файлы страниц
        for (ChapterPage page : chapter.getPages()) {
            fileStorage.delete(CHAPTERS_DIR, page.getFileName());
        }

        chapterRepository.delete(chapter);
    }


    private ComicChapterDto toDto(ComicChapter chapter, boolean withPages) {
        List<ChapterPageDto> pages = withPages
            ? chapter.getPages().stream().map(this::pageToDto).toList()
            : List.of();

        String createdAt = chapter.getCreatedAt() != null
            ? chapter.getCreatedAt().toString()
            : null;

        return new ComicChapterDto(
            chapter.getId(),
            chapter.getChapterNumber(),
            chapter.getTitle(),
            chapter.getPages().size(),
            createdAt,
            pages
        );
    }

    private ChapterPageDto pageToDto(ChapterPage page) {
        String url = "/api/images/chapters/" + page.getFileName();
        return new ChapterPageDto(
            page.getId(), url,
            page.getOriginalName(),
            page.getSortOrder()
        );
    }
}
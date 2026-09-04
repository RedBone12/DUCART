package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.utils.FileUploadUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class GenericCatalogService<T> {
    private final JpaRepository<T, Long> repo;
    private final String uploadFolder;
    private final Class<T> type;

    public GenericCatalogService(JpaRepository<T, Long> repo, String uploadFolder, Class<T> type) {
        this.repo = repo;
        this.uploadFolder = uploadFolder;
        this.type = type;
    }

    public T create(T item, MultipartFile pic) throws IOException {
        setPicIfPossible(item, FileUploadUtil.save(pic, uploadFolder));
        return repo.save(item);
    }

    public T createWithoutFile(T item) {
        return repo.save(item);
    }

    public List<T> findAll() { return repo.findAll(); }

    public T findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException(type.getSimpleName() + " not found"));
    }

    public T update(Long id, T item, MultipartFile pic) throws IOException {
        T existing = findById(id);
        String oldPic = getPicIfPossible(existing);
        BeanUtils.copyProperties(item, existing, "id");
        String newPic = FileUploadUtil.save(pic, uploadFolder);
        setPicIfPossible(existing, newPic != null ? newPic : oldPic);
        return repo.save(existing);
    }

    public T updatePlain(Long id, T item) {
        T existing = findById(id);
        String oldPic = getPicIfPossible(existing);
        BeanUtils.copyProperties(item, existing, "id");
        if (getPicIfPossible(existing) == null) setPicIfPossible(existing, oldPic);
        return repo.save(existing);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException(type.getSimpleName() + " not found");
        repo.deleteById(id);
    }

    private void setPicIfPossible(T item, String pic) {
        if (pic == null) return;
        try {
            Method m = item.getClass().getMethod("setPic", String.class);
            m.invoke(item, pic);
        } catch (Exception ignored) {}
    }

    private String getPicIfPossible(T item) {
        try {
            Method m = item.getClass().getMethod("getPic");
            Object value = m.invoke(item);
            return value == null ? null : value.toString();
        } catch (Exception ignored) { return null; }
    }
}

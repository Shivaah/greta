package com.cnam.greta.data.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

public interface BaseRepository<T> {

    LiveData<Long> insert(final T object);
    LiveData<Long[]> insert(final List<T> object);
    LiveData<T> get(final long id);
    LiveData<List<T>> get();
    void update(final T object);
    void update(final List<T> object);
    void delete(final long id);
    void delete(final T object);
    void delete(final List<T> object);

}

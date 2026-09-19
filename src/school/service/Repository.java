package school.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import school.model.SchoolData.Identifiable;

public interface Repository<T extends Identifiable> {

    void save(T item);

    T findById(String id);

    List<T> findAll();

    boolean delete(String id);

    class InMemory<T extends Identifiable> implements Repository<T> {

        private Map<String, T> records = new HashMap<>();

        @Override
        public void save(T item) {

            if (records.containsKey(item.getId())) {
                throw new IllegalArgumentException("Duplicate ID");
            }

            records.put(item.getId(), item);
        }

        @Override
        public T findById(String id) {
            return records.get(id);
        }

        @Override
        public List<T> findAll() {
            return new ArrayList<>(records.values());
        }

        @Override
        public boolean delete(String id) {
            return records.remove(id) != null;
        }
    }
} 
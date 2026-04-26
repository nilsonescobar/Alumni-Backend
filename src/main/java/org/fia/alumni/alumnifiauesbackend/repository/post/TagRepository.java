package org.fia.alumni.alumnifiauesbackend.repository.post;

import org.fia.alumni.alumnifiauesbackend.entity.post.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}
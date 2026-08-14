package com.likelion.firstbite.firstbiteserver.sidemenu.repository;

import com.likelion.firstbite.firstbiteserver.sidemenu.domain.SideMenu;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SideMenuRepository extends JpaRepository<SideMenu, UUID> {
    @EntityGraph(attributePaths = "food")
    List<SideMenu> findAllByActiveTrue();

    @EntityGraph(attributePaths = "food")
    Optional<SideMenu> findByIdAndActiveTrue(UUID id);
}

package com.finanzas.backend.service;

import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.CategoryType;
import com.finanzas.backend.repo.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DefaultCategoryService {
    private final CategoryRepository categories;

    public DefaultCategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    public void seed(UUID workspaceId) {
        seed(workspaceId, "Salario", CategoryType.INCOME, "ING", "#34a853");
        seed(workspaceId, "Freelance", CategoryType.INCOME, "FRE", "#1a73e8");
        seed(workspaceId, "Negocio", CategoryType.INCOME, "NEG", "#1a73e8");
        seed(workspaceId, "Otros", CategoryType.INCOME, "OTR", "#64748b");

        seed(workspaceId, "Alimentacion", CategoryType.EXPENSE, "ALI", "#f59e0b");
        seed(workspaceId, "Vivienda", CategoryType.EXPENSE, "VIV", "#1a73e8");
        seed(workspaceId, "Transporte", CategoryType.EXPENSE, "TRA", "#6366f1");
        seed(workspaceId, "Salud", CategoryType.EXPENSE, "SAL", "#ef4444");
        seed(workspaceId, "Educacion", CategoryType.EXPENSE, "EDU", "#14b8a6");
        seed(workspaceId, "Entretenimiento", CategoryType.EXPENSE, "ENT", "#8b5cf6");
        seed(workspaceId, "Servicios", CategoryType.EXPENSE, "SER", "#0ea5e9");
        seed(workspaceId, "Suscripciones", CategoryType.EXPENSE, "SUB", "#475569");
        seed(workspaceId, "Deudas", CategoryType.EXPENSE, "DEU", "#b91c1c");
        seed(workspaceId, "Otros", CategoryType.EXPENSE, "OTR", "#64748b");
    }

    private void seed(UUID workspaceId, String name, CategoryType type, String icon, String color) {
        if (categories.findByWorkspaceIdAndNombreIgnoreCaseAndType(workspaceId, name, type).isEmpty()) {
            categories.save(new CategoryEntity(workspaceId, name, type, icon, color));
        }
    }
}

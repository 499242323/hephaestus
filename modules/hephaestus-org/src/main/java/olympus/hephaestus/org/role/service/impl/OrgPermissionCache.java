package olympus.hephaestus.org.role.service.impl;

import olympus.hephaestus.org.role.config.OrgPermissionCacheConfig;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class OrgPermissionCache {

    private final OrgPermissionSnapshotLoader snapshotLoader;
    private final CacheManager cacheManager;

    public OrgPermissionCache(OrgPermissionSnapshotLoader snapshotLoader,
                              CacheManager cacheManager) {
        this.snapshotLoader = snapshotLoader;
        this.cacheManager = cacheManager;
    }

    public PermissionSnapshot getSnapshot(Long personId) {
        if (personId == null) {
            return PermissionSnapshot.empty();
        }
        return snapshotLoader.loadSnapshot(personId);
    }

    public void evict(Long personId) {
        if (personId != null) {
            cache().evict(personId);
        }
    }

    public void evict(Collection<Long> personIds) {
        if (personIds == null || personIds.isEmpty()) {
            return;
        }
        personIds.forEach(this::evict);
    }

    public void evictAfterCommit(Long personId) {
        if (personId == null) {
            return;
        }
        runAfterCommit(() -> evict(personId));
    }

    public void evictAfterCommit(Collection<Long> personIds) {
        if (personIds == null || personIds.isEmpty()) {
            return;
        }
        List<Long> distinctPersonIds = personIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (distinctPersonIds.isEmpty()) {
            return;
        }
        runAfterCommit(() -> evict(distinctPersonIds));
    }

    private Cache cache() {
        Cache cache = cacheManager.getCache(OrgPermissionCacheConfig.PERSON_PERMISSION_CACHE);
        if (cache == null) {
            throw new IllegalStateException("Missing cache: " + OrgPermissionCacheConfig.PERSON_PERMISSION_CACHE);
        }
        return cache;
    }

    private void runAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    public record PermissionSnapshot(boolean admin, Set<String> permissionCodes) {

        private static PermissionSnapshot empty() {
            return new PermissionSnapshot(false, Set.of());
        }
    }
}

// service/LocationService.java (Interface)
package com.helpme.backend.service;

import org.locationtech.jts.geom.Point;
import java.util.List;
import java.util.UUID;

public interface LocationService {
    void updateLocation(UUID providerId, double lat, double lng);

    List<UUID> findNearby(double lat, double lng, int radiusMeters);

    void removeLocation(UUID providerId);

    Point getLocation(UUID providerId);

    boolean hasLiveLocation(UUID providerId);
}
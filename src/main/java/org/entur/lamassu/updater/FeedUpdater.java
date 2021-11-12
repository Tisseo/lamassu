/*
 *
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *
 */

package org.entur.lamassu.updater;

import org.entur.gbfs.GbfsDelivery;
import org.entur.gbfs.GbfsSubscriptionManager;
import org.entur.gbfs.GbfsSubscriptionOptions;
import org.entur.gbfs.v2_2.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_2.gbfs.GBFS;
import org.entur.gbfs.v2_2.gbfs.GBFSFeedName;
import org.entur.gbfs.v2_2.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v2_2.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_2.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_2.system_alerts.GBFSSystemAlerts;
import org.entur.gbfs.v2_2.system_calendar.GBFSSystemCalendar;
import org.entur.gbfs.v2_2.system_hours.GBFSSystemHours;
import org.entur.gbfs.v2_2.system_information.GBFSSystemInformation;
import org.entur.gbfs.v2_2.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v2_2.system_regions.GBFSSystemRegions;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleTypes;
import org.entur.lamassu.cache.GBFSFeedCacheV2;
import org.entur.lamassu.config.feedprovider.FeedProviderConfig;
import org.entur.lamassu.mapper.feedmapper.FeedMapper;
import org.entur.lamassu.model.provider.FeedProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.ForkJoinPool;

@Component
public class FeedUpdater {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final FeedProviderConfig feedProviderConfig;
    private final GBFSFeedCacheV2 feedCache;
    private final FeedMapper<GBFS> discoveryFeedMapper;
    private final FeedMapper<GBFSSystemInformation> systemInformationFeedMapper;
    private final FeedMapper<GBFSSystemAlerts> systemAlertsFeedMapper;
    private final FeedMapper<GBFSSystemCalendar> systemCalendarFeedMapper;
    private final FeedMapper<GBFSSystemRegions> systemRegionsFeedMapper;
    private final FeedMapper<GBFSSystemPricingPlans> systemPricingPlansFeedMapper;
    private final FeedMapper<GBFSSystemHours> systemHoursFeedMapper;
    private final FeedMapper<GBFSVehicleTypes> vehicleTypesFeedMapper;
    private final FeedMapper<GBFSGeofencingZones> geofencingZonesFeedMapper;
    private final FeedMapper<GBFSStationInformation> stationInformationFeedMapper;
    private final FeedMapper<GBFSStationStatus> stationStatusFeedMapper;
    private final FeedMapper<GBFSFreeBikeStatus> freeBikeStatusFeedMapper;

    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    private GbfsSubscriptionManager subscriptionManager;
    private ForkJoinPool updaterThreadPool;

    @Autowired
    public FeedUpdater(
            FeedProviderConfig feedProviderConfig,
            GBFSFeedCacheV2 feedCache,
            FeedMapper<GBFS> discoveryFeedMapper,
            FeedMapper<GBFSSystemInformation> systemInformationFeedMapper,
            FeedMapper<GBFSSystemAlerts> systemAlertsFeedMapper,
            FeedMapper<GBFSSystemCalendar> systemCalendarFeedMapper,
            FeedMapper<GBFSSystemRegions> systemRegionsFeedMapper,
            FeedMapper<GBFSSystemPricingPlans> systemPricingPlansFeedMapper,
            FeedMapper<GBFSSystemHours> systemHoursFeedMapper,
            FeedMapper<GBFSVehicleTypes> vehicleTypesFeedMapper,
            FeedMapper<GBFSGeofencingZones> geofencingZonesFeedMapper,
            FeedMapper<GBFSStationInformation> stationInformationFeedMapper,
            FeedMapper<GBFSStationStatus> stationStatusFeedMapper,
            FeedMapper<GBFSFreeBikeStatus> freeBikeStatusFeedMapper
    ) {
        this.feedProviderConfig = feedProviderConfig;
        this.feedCache = feedCache;
        this.discoveryFeedMapper = discoveryFeedMapper;
        this.systemInformationFeedMapper = systemInformationFeedMapper;
        this.systemAlertsFeedMapper = systemAlertsFeedMapper;
        this.systemCalendarFeedMapper = systemCalendarFeedMapper;
        this.systemRegionsFeedMapper = systemRegionsFeedMapper;
        this.systemPricingPlansFeedMapper = systemPricingPlansFeedMapper;
        this.systemHoursFeedMapper = systemHoursFeedMapper;
        this.vehicleTypesFeedMapper = vehicleTypesFeedMapper;
        this.geofencingZonesFeedMapper = geofencingZonesFeedMapper;
        this.stationInformationFeedMapper = stationInformationFeedMapper;
        this.stationStatusFeedMapper = stationStatusFeedMapper;
        this.freeBikeStatusFeedMapper = freeBikeStatusFeedMapper;
    }

    public void start() {
        updaterThreadPool = new ForkJoinPool(NUM_CORES * 2);
        subscriptionManager = new GbfsSubscriptionManager(updaterThreadPool);
        updaterThreadPool.submit(this::createSubscriptions);
    }

    public void update() {
        subscriptionManager.update();
    }

    public void stop() {
        updaterThreadPool.shutdown();
    }

    private void createSubscriptions() {
        feedProviderConfig.getProviders().parallelStream().forEach(this::createSubscription);
    }

    private void createSubscription(FeedProvider feedProvider) {
        var options = new GbfsSubscriptionOptions();
        options.setDiscoveryURI(URI.create(feedProvider.getUrl()));
        options.setLanguageCode(feedProvider.getLanguage());
        if (feedProvider.getAuthentication() != null) {
            options.setRequestAuthenticator(feedProvider.getAuthentication().getRequestAuthenticator());
        }
        subscriptionManager.subscribe(options, delivery -> updateFeedCaches(feedProvider, delivery));
    }

    private void updateFeedCaches(FeedProvider feedProvider, GbfsDelivery delivery) {
        updateFeedCache(feedProvider, GBFSFeedName.GBFS, discoveryFeedMapper.map(delivery.getDiscovery(), feedProvider));

        // Lamassu currently only support producing a single version of GBFS, therefore
        // mapping of the versions file, if it exists, is intentionally skipped.
        //updateFeedCache(feedProvider, GBFSFeedName.GBFSVersions, delivery.getVersion()); //NOSONAR

        updateFeedCache(feedProvider, GBFSFeedName.SystemInformation, systemInformationFeedMapper.map(delivery.getSystemInformation(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.SystemAlerts, systemAlertsFeedMapper.map(delivery.getSystemAlerts(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.SystemCalendar, systemCalendarFeedMapper.map(delivery.getSystemCalendar(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.SystemRegions, systemRegionsFeedMapper.map(delivery.getSystemRegions(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.SystemPricingPlans, systemPricingPlansFeedMapper.map(delivery.getSystemPricingPlans(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.SystemHours, systemHoursFeedMapper.map(delivery.getSystemHours(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.VehicleTypes, vehicleTypesFeedMapper.map(delivery.getVehicleTypes(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.GeofencingZones, geofencingZonesFeedMapper.map(delivery.getGeofencingZones(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.StationInformation, stationInformationFeedMapper.map(delivery.getStationInformation(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.StationStatus, stationStatusFeedMapper.map(delivery.getStationStatus(), feedProvider));
        updateFeedCache(feedProvider, GBFSFeedName.FreeBikeStatus, freeBikeStatusFeedMapper.map(delivery.getFreeBikeStatus(), feedProvider));
    }

    private void updateFeedCache(FeedProvider feedProvider, GBFSFeedName feedName, Object feed) {
        if (feed != null) {
            logger.info("updating feed {} for provider {}", feedName, feedProvider.getSystemId());
            logger.trace("updating feed {} for provider {} data {}", feedName, feedProvider.getSystemId(), feed);
            feedCache.update(feedName, feedProvider, feed);
        } else {
            logger.debug("no feed {} found for provider {}", feedName, feedProvider.getSystemId());
        }
    }


}

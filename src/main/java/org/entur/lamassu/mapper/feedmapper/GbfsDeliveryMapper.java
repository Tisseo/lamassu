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

package org.entur.lamassu.mapper.feedmapper;

import org.entur.gbfs.loader.v2.GbfsV2Delivery;
import org.entur.gbfs.v2_3.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_3.gbfs.GBFS;
import org.entur.gbfs.v2_3.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v2_3.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_3.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_3.system_alerts.GBFSSystemAlerts;
import org.entur.gbfs.v2_3.system_calendar.GBFSSystemCalendar;
import org.entur.gbfs.v2_3.system_hours.GBFSSystemHours;
import org.entur.gbfs.v2_3.system_information.GBFSSystemInformation;
import org.entur.gbfs.v2_3.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v2_3.system_regions.GBFSSystemRegions;
import org.entur.gbfs.v2_3.vehicle_types.GBFSVehicleTypes;
import org.entur.lamassu.model.provider.FeedProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GbfsDeliveryMapper {

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

  @Autowired
  public GbfsDeliveryMapper(
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

  public GbfsV2Delivery mapGbfsDelivery(
    GbfsV2Delivery delivery,
    FeedProvider feedProvider
  ) {
    var mappedVehicleTypes = vehicleTypesFeedMapper.map(
      delivery.vehicleTypes(),
      feedProvider
    );
    return new GbfsV2Delivery(
      discoveryFeedMapper.map(delivery.discovery(), feedProvider),
      // Lamassu currently only support producing a single version of GBFS, therefore
      // mapping of the versions file, if it exists, is intentionally skipped.
      null,
      systemInformationFeedMapper.map(delivery.systemInformation(), feedProvider),
      mappedVehicleTypes,
      stationInformationFeedMapper.map(delivery.stationInformation(), feedProvider),
      stationStatusFeedMapper.map(
        delivery.stationStatus(),
        feedProvider,
        stationStatus ->
          VehicleTypeCapacityProducer.addToStations(stationStatus, mappedVehicleTypes)
      ),
      freeBikeStatusFeedMapper.map(delivery.freeBikeStatus(), feedProvider),
      systemHoursFeedMapper.map(delivery.systemHours(), feedProvider),
      systemCalendarFeedMapper.map(delivery.systemCalendar(), feedProvider),
      systemRegionsFeedMapper.map(delivery.systemRegions(), feedProvider),
      systemPricingPlansFeedMapper.map(delivery.systemPricingPlans(), feedProvider),
      systemAlertsFeedMapper.map(delivery.systemAlerts(), feedProvider),
      geofencingZonesFeedMapper.map(delivery.geofencingZones(), feedProvider),
      null
    );
  }
}

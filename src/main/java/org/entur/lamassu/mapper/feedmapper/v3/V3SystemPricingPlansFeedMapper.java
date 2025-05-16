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

package org.entur.lamassu.mapper.feedmapper.v3;

import static org.entur.lamassu.mapper.feedmapper.IdMappers.PRICING_PLAN_ID_TYPE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.entur.lamassu.mapper.feedmapper.AbstractFeedMapper;
import org.entur.lamassu.mapper.feedmapper.IdMappers;
import org.entur.lamassu.model.provider.FeedProvider;
import org.mobilitydata.gbfs.v3_0.system_pricing_plans.GBFSDescription;
import org.mobilitydata.gbfs.v3_0.system_pricing_plans.GBFSData;
import org.mobilitydata.gbfs.v3_0.system_pricing_plans.GBFSName;
import org.mobilitydata.gbfs.v3_0.system_pricing_plans.GBFSPlan;
import org.mobilitydata.gbfs.v3_0.system_pricing_plans.GBFSSystemPricingPlans;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class V3SystemPricingPlansFeedMapper
  extends AbstractFeedMapper<GBFSSystemPricingPlans> {

  private static final String TARGET_GBFS_VERSION = "3.0";

  @Value("${org.entur.lamassu.targetGbfsVersion:2.2}")
  private String targetGbfsVersion;

  @Override
  public GBFSSystemPricingPlans map(
    GBFSSystemPricingPlans source,
    FeedProvider feedProvider
  ) {
    if (feedProvider.getPricingPlans() != null) {
      return customPricingPlans(feedProvider);
    }

    if (
      source == null || source.getData() == null || source.getData().getPlans() == null
    ) {
      return null;
    }

    var mapped = new GBFSSystemPricingPlans();
    mapped.setVersion(TARGET_GBFS_VERSION);
    mapped.setTtl(source.getTtl());
    mapped.setLastUpdated(source.getLastUpdated());
    mapped.setData(mapData(source.getData(), feedProvider));
    return mapped;
  }


  private GBFSSystemPricingPlans customPricingPlans(FeedProvider feedProvider) {
    var custom = new GBFSSystemPricingPlans();
    custom.setVersion(targetGbfsVersion);
    custom.setLastUpdated(new Date());
    custom.setTtl((int) Duration.ofMinutes(5).toSeconds());
    var data = new GBFSData();
    List<GBFSPlan> plans = convertPlans(feedProvider);
    data.setPlans(plans);
    custom.setData(mapData(data, feedProvider));
    return custom;
  }


  private List<GBFSPlan> convertPlans(FeedProvider feedProvider) {
    List<GBFSPlan> plans = new ArrayList<>();
    for (org.mobilitydata.gbfs.v2_3.system_pricing_plans.GBFSPlan plan : feedProvider.getPricingPlans()) {
        GBFSPlan newPlan = new GBFSPlan();
        newPlan.setPlanId(plan.getPlanId());
        newPlan.setCurrency(plan.getCurrency());
        newPlan.setPrice(plan.getPrice());

        List<GBFSDescription> descriptions = new ArrayList<>();
        GBFSDescription newDescription = new GBFSDescription();
        newDescription.setLanguage("fr");
        newDescription.setText(plan.getDescription());
        newPlan.setDescription(descriptions);

        List<GBFSName> names = new ArrayList<>();
        GBFSName newName = new GBFSName();
        newName.setLanguage("fr");
        newName.setText( plan.getName());
        names.add(newName);
        newPlan.setName(names);

        plans.add(newPlan);
    }
    return plans;
  }

  private GBFSData mapData(GBFSData data, FeedProvider feedProvider) {
    var mapped = new GBFSData();
    var plans = mapPlans(data.getPlans(), feedProvider);
    mapped.setPlans(plans);
    return mapped;
  }

  private List<GBFSPlan> mapPlans(List<GBFSPlan> plans, FeedProvider feedProvider) {
    return plans
      .stream()
      .map(plan -> mapPlan(plan, feedProvider))
      .collect(Collectors.toList());
  }

  private GBFSPlan mapPlan(GBFSPlan plan, FeedProvider feedProvider) {
    var mapped = new GBFSPlan();
    mapped.setPlanId(
      IdMappers.mapId(feedProvider.getCodespace(), PRICING_PLAN_ID_TYPE, plan.getPlanId())
    );
    mapped.setUrl(plan.getUrl());
    mapped.setName(plan.getName());
    mapped.setDescription(plan.getDescription());
    mapped.setCurrency(plan.getCurrency());
    mapped.setIsTaxable(plan.getIsTaxable());
    mapped.setPrice(plan.getPrice());
    mapped.setSurgePricing(plan.getSurgePricing());
    mapped.setPerKmPricing(plan.getPerKmPricing());
    mapped.setPerMinPricing(plan.getPerMinPricing());
    return mapped;
  }
}

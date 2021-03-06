/*
 * SonarQube Issue Assign Plugin
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.issueassign.measures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.plugins.issueassign.exception.MissingScmMeasureDataException;

import java.util.HashMap;
import java.util.Map;

public class MeasuresCollector implements Decorator {

  private static final Logger LOG = LoggerFactory.getLogger(MeasuresCollector.class);
  private final Map<String, ScmMeasures> resourceScmMeasures = new HashMap<String, ScmMeasures>();
  private final Settings settings;

  public MeasuresCollector(final Settings settings) {
    this.settings = settings;
  }

  public void decorate(final Resource resource, final DecoratorContext decoratorContext) {

    if (ResourceUtils.isFile(resource)) {
      try {
        final ScmMeasures scmMeasures = this.getMeasures(resource.getEffectiveKey(), decoratorContext);
        resourceScmMeasures.put(resource.getEffectiveKey(), scmMeasures);
      } catch (final MissingScmMeasureDataException e) {
        LOG.warn("SCM Measures not collected for resource [" + resource.getEffectiveKey() + "]");
      } catch (final Exception e) {
        LOG.error("Error collecting measures for resource [" + resource.getEffectiveKey() + "]", e);
      }
    }
  }

  public boolean shouldExecuteOnProject(final Project project) {
    final boolean isEnabled = this.settings.getBoolean(org.sonar.plugins.issueassign.IssueAssignPlugin.PROPERTY_ENABLED);
    LOG.info("Issue Assign Plugin is " + (isEnabled ? "ENABLED" : "DISABLED"));
    return isEnabled;
  }

  public Map<String, ScmMeasures> getResources() {
    return this.resourceScmMeasures;
  }

  private ScmMeasures getMeasures(final String resourceKey, final DecoratorContext decoratorContext) throws MissingScmMeasureDataException {
    final String authorsByLineMeasureData = this.getMeasureData(decoratorContext, CoreMetrics.SCM_AUTHORS_BY_LINE, resourceKey);
    final String lastCommitByLineMeasureData = this.getMeasureData(decoratorContext, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, resourceKey);
    final String revisionsByLineMeasureData = this.getMeasureData(decoratorContext, CoreMetrics.SCM_REVISIONS_BY_LINE, resourceKey);

    return new ScmMeasures(resourceKey, authorsByLineMeasureData,
        lastCommitByLineMeasureData, revisionsByLineMeasureData);
  }

  private String getMeasureData(final DecoratorContext decoratorContext, final Metric metric, final String resourceKey) throws MissingScmMeasureDataException {
    final Measure measure = decoratorContext.getMeasure(metric);
    if (MeasureUtils.hasData(measure)) {
      return measure.getData();
    }
    LOG.debug("No measure found for metric [" + metric.getKey() + "] on resource [" + resourceKey + "]");
    throw new MissingScmMeasureDataException();
  }
}

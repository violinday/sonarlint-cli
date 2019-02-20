/*
 * SonarLint CLI
 * Copyright (C) 2016-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.cli.analysis;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarlint.cli.report.Severity;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.tracking.IssueTrackable;
import org.sonarsource.sonarlint.core.tracking.Trackable;

public class StandaloneSonarLint extends SonarLint {
  private final StandaloneSonarLintEngine engine;

  public StandaloneSonarLint(StandaloneSonarLintEngine engine) {
    this.engine = engine;
  }

  @Override
  protected void doAnalysis(Map<String, String> properties, ReportFactory reportFactory, List<ClientInputFile> inputFiles, Path baseDirPath, String severityLevel) {
    Date start = new Date();

    IssueCollector collector = new IssueCollector();
    StandaloneAnalysisConfiguration config = new StandaloneAnalysisConfiguration(baseDirPath, baseDirPath.resolve(".sonarlint"),
      inputFiles, properties);
    AnalysisResults result = engine.analyze(config, collector);
    Collection<Trackable> trackables = collector.get().stream().map(IssueTrackable::new).collect(Collectors.toList());
    Iterator<Trackable> it = trackables.iterator();

    Severity severity = Severity.valueOf(severityLevel);
    while (it.hasNext()) {
      if (Severity.valueOf(it.next().getSeverity()).ordinal() < severity.ordinal()) {
        it.remove();
      }
    }
    generateReports(trackables, result, reportFactory, baseDirPath.getFileName().toString(), baseDirPath, start);
  }

  @Override
  protected RuleDetails getRuleDetails(String ruleKey) {
    return engine.getRuleDetails(ruleKey);
  }

  @Override
  public void stop() {
    engine.stop();
  }
}

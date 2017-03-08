/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import javax.servlet.ServletContext

import nl.knaw.dans.easy.license.app.{LicenseCreatorService, LicenseCreatorServlet}
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle with DebugEnhancedLogging {
  override def init(context: ServletContext) {
    trace(context)
    context.mount(new LicenseCreatorServlet, "/")
  }
}

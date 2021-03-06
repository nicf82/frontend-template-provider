/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.frontendtemplateprovider.controllers

import play.api.Play
import play.api.mvc._
import uk.gov.hmrc.play.config.{AssetsConfig, ServicesConfig}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

object GovUkTemplateRendererController extends GovUkTemplateRendererController

trait GovUkTemplateRendererController extends BaseController with ServicesConfig {

	val assetsPrefix: String = AssetsConfig.assetsPrefix

	def serveMustacheTemplate(): Action[AnyContent] = Action.async { implicit request =>
		val templateLocation = Play.current.configuration.getString("template.url").getOrElse("")
		val resolveUrl: (String) => String = a => s"${templateLocation}assets/$a"

		Future.successful(Ok(views.html.Application.gov_main_mustache(resolveUrl, assetsPrefix + "/")))
	}

}

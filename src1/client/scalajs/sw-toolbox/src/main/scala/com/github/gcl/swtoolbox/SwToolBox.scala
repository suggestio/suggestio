package com.github.gcl.swtoolbox

import org.scalajs.dom.experimental.serviceworkers.CacheQueryOptions

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.18 11:21
  * Description: sw-toolbox API facade.
  */
@JSImport("sw-toolbox", JSImport.Namespace)
@js.native
object SwToolBox extends js.Object {

  def router: SwTbRouter = js.native

  def networkFirst: RouterHandlerF = js.native
  def cacheFirst: RouterHandlerF = js.native
  def fastest: RouterHandlerF = js.native
  def cacheOnly: RouterHandlerF = js.native
  def networkOnly: RouterHandlerF = js.native

  def precache(urls: js.Array[String]): Unit = js.native

  def cache(url: String, options: SwTbRouteOptions = js.native): Unit = js.native
  def uncache(url: String, options: SwTbRouteOptions = js.native): Unit = js.native

}


@js.native
trait SwTbRouter extends js.Object {

  def get(regExp: String, handler: RouterHandlerF, options: SwTbRouteOptions = js.native): Unit = js.native
  def post(regExp: String, handler: RouterHandlerF, options: SwTbRouteOptions = js.native): Unit = js.native
  def put(regExp: String, handler: RouterHandlerF, options: SwTbRouteOptions = js.native): Unit = js.native
  def delete(regExp: String, handler: RouterHandlerF, options: SwTbRouteOptions = js.native): Unit = js.native
  def head(regExp: String, handler: RouterHandlerF, options: SwTbRouteOptions = js.native): Unit = js.native
  def any(regExp: String, handler: RouterHandlerF, options: SwTbRouteOptions = js.native): Unit = js.native

  var default: RouterHandlerF = js.native

}


trait SwTbRouteOptions extends js.Object {
  val debug: js.UndefOr[Boolean] = js.undefined
  val networkTimeoutSeconds: js.UndefOr[Double] = js.undefined
  val cache: js.UndefOr[SwTbCachingOptions] = js.undefined
}


trait SwTbCachingOptions extends js.Object {
  val name: js.UndefOr[String] = js.undefined
  val maxEntries: js.UndefOr[Int] = js.undefined
  val maxAgeSeconds: js.UndefOr[Int] = js.undefined
  val queryOptions: js.UndefOr[CacheQueryOptions] = js.undefined
}

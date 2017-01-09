package io.suggest.sc.sjs.m.msearch

import io.suggest.common.menum.{LightEnumeration, StrIdValT}
import io.suggest.sc.sjs.vm.search.tabs.geo.{SGeoRoot, SGeoTabBtn}
import io.suggest.sc.sjs.vm.search.tabs.htag.{ShtRoot, ShtTabBtn}
import io.suggest.sc.sjs.vm.search.tabs.{TabBtnCompanion, TabRootCompanion}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 16:08
 * Description: Модель вкладок поисковой панели.
 * Нужна для простого абстрактного доступа к оным.
 */
object MTabs extends LightEnumeration with StrIdValT {

  /** Интерфейс экземпляров модели. Можно вынести его за пределы [[MTabs]]. */
  protected[this] trait ValT extends super.ValT {

    /** Объект-компаньон для тела таба. */
    def vmBodyCompanion: TabRootCompanion
    /** Объект-компаньон для кнопки таба. */
    def vmBtnCompanion: TabBtnCompanion

  }

  /** Абстрактый класс элементов модели. */
  abstract protected[this] class Val extends ValT

  override type T = Val

  /** Таб с геопоиском. */
  val Geo: T = new Val {
    override def strId = "g"
    override def vmBtnCompanion = SGeoTabBtn
    override def vmBodyCompanion = SGeoRoot
  }

  /** Таб со списком хеш-тегов и поиском по ним. */
  val Tags: T = new Val {
    override def strId = "h"
    override def vmBodyCompanion = ShtRoot
    override def vmBtnCompanion  = ShtTabBtn
  }


  /** Десериализация из id. */
  override def maybeWithName(n: String): Option[T] = {
    if (n == Geo.strId) {
      Some(Geo)
    } else if (n == Tags.strId) {
      Some(Tags)
    } else {
      None
    }
  }

  def values = Seq[T](Geo, Tags)

}

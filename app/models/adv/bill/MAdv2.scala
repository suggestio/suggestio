package models.adv.bill

import java.sql.Connection

import anorm._
import com.google.inject.assistedinject.Assisted
import com.google.inject.{Inject, ImplementedBy, Singleton}
import util.sqlm.SqlModelSave

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.15 15:24
 * Description: Унифицированная модель биллинга рекламных размещений.
 * Пришла на смену MAdv (с её зоопарком подмоделей).
 * Пришла следом за MNode для узлов-карточек-картинок.
 * Модель содержит в себе кучу таблиц, но тут живёт в одном файле.
 * Свойства конкретного adv вынесены в подмодель.
 *
 * MAdv2_ содержит статическую сторону модели, инжектируемую через DI.
 * MAdv2T_ содержит трейт (интерфейс) в целях разруливания циркулярной инжекции компаньона в factoty.
 */

@ImplementedBy( classOf[MAdv2_] )
trait MAdv2T_ {

}


@Singleton
/** Реализация статической стороны модели размещений карточек. */
class MAdv2_ @Inject() (factory: MAdv2FactoryWrapper)
  extends MAdv2T_


trait MAdv2Factory {

  def apply(
    common        : MAdv2Common,
    scRcvr        : Option[ScRcvr],
    id            : Option[Int]
  ): MAdv2

}


/** Внутри factory.apply нельзя объявить дефолтовые значения,
  * поэтому тут враппер для вызова factory.apply() метода. */
class MAdv2FactoryWrapper @Inject() (fac: MAdv2Factory) {

  def apply(
    common        : MAdv2Common,
    scRcvr        : Option[ScRcvr]  = None,
    id            : Option[Int]     = None
  ): MAdv2 = {
    fac(common, scRcvr, id)
  }

}


/** Динамический экземпляр модели. */
case class MAdv2 @Inject() (
  @Assisted common        : MAdv2Common,
  companion               : MAdv2T_,
  @Assisted scRcvr        : Option[ScRcvr]  = None,
  @Assisted id            : Option[Int]     = None
)
  extends SqlModelSave
{
  override type T = MAdv2
  override def hasId: Boolean = id.nonEmpty

  override def saveInsert(implicit c: Connection): T = {
    // TODO Сгенерить INSERT-запрос на основе подмоделей и опционального id.
    SQL {
      "INSERT INTO " + common.mode.tableName + "(" +
        ??? +
        ") VALUES (" +
        ??? +
        ")"
    }
    .on(???)
    .executeInsert(???)
  }

  override def saveUpdate(implicit c: Connection): Int = ???

}


package io.suggest.model.n2.extra.search

import io.suggest.common.empty.EmptyProduct
import io.suggest.model.n2.node.MNodeFields.Extras.{BEACON_MAJOR_FN, BEACON_MINOR_FN, BEACON_UUID_FN}
import io.suggest.model.search.{DynSearchArgs, DynSearchArgsWrapper}
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 17:52
  * Description: Трейт для поддержки поиска по id маячков.
  */
trait BleBeacons extends DynSearchArgs {

  /**
    * Список поисковых критериев для выборки маячков.
    * Если несколько критериев, то они объединяются в запрос через ИЛИ. Так можно искать сразу много маячков.
    * @return Список критериев в произвольном порядке.
    */
  def bleBeacons: Iterable[BleBeaconCriteria]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qOpt0 = super.toEsQueryOpt

    // crs == criterias.
    val crs = bleBeacons
      .filter(_.nonEmpty)

    if (crs.nonEmpty) {
      // Есть хотя бы один непустой критерий для поиска BLE-маячка. Начать сборку внешнего запроса.
      val outerQ = QueryBuilders.boolQuery()
        .minimumNumberShouldMatch(1)

      // Закинуть в собираемую outerQ инстанс исходной query, если она определена.
      for (q0 <- qOpt0)
        outerQ.must(q0)

      // Собирать и закидывать в outerQ запросы по каждому критерию:
      for (cr <- crs) {
        val crQ = QueryBuilders.boolQuery()

        // Отработать значения proximity UUIDs текущего критерия
        if (cr.uuids.nonEmpty) {
          val uuidsQ = QueryBuilders.termsQuery(BEACON_UUID_FN, cr.uuids: _*)
          crQ.must(uuidsQ)
        }

        // Отработать major ids текущего критерия.
        if (cr.majors.nonEmpty) {
          val majorsQ = QueryBuilders.termsQuery(BEACON_MAJOR_FN, cr.majors: _*)
          crQ.must(majorsQ)
        }

        // Отработать minor ids текущего критерия.
        if (cr.minors.nonEmpty) {
          val minorsQ = QueryBuilders.termsQuery(BEACON_MINOR_FN, cr.minors: _*)
          crQ.must(minorsQ)
        }

        // Закинуть собранную crQ в общий аккамулятор
        outerQ.should(crQ)
      }

      // Вернуть свежесобранную query.
      Some(outerQ)

    } else {
      // Неопределено никаких критериев для поиска BLE-маячков.
      qOpt0
    }
  }

}

/** Дефолтовая реализация аддона [[BleBeacons]]. */
trait BleBeaconsDflt extends BleBeacons {
  override def bleBeacons: Iterable[BleBeaconCriteria] = Nil
}

/** Wrap-реализация аддона [[BleBeacons]]. */
trait BleBeaconsWrap extends BleBeacons with DynSearchArgsWrapper {
  override type WT <: BleBeacons
  override def bleBeacons = _dsArgsUnderlying.bleBeacons
}


/**
  * Модель одного критерия поиска маячков.
  * @param uuids Список искомых proximity UUIDs.
  * @param majors Список допустимых значений Major-поля.
  * @param minors Список допустимых значений Minor-поля.
  */
case class BleBeaconCriteria(
  uuids   : Seq[String] = Nil,
  majors  : Seq[Int]    = Nil,
  minors  : Seq[Int]    = Nil
)
  extends EmptyProduct

package io.suggest.ym.model.common

import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import io.suggest.model.sc.common.{AdShowLevels, AdShowLevel}
import play.api.libs.json.{JsNumber, JsObject}
import java.{util => ju}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.01.16 18:43
 * Description:
 */



object ShowLevelLimits {

  type LvlMap_t = Map[AdShowLevel, Int]

  val IN_ESFN = "in"
  val OUT_ESFN = "out"

  /** Функция-десериализатор карты уровней. */
  val deserializeLevelsMap: PartialFunction[Any, LvlMap_t] = {
    case null =>
      Map.empty

    case rawLevelsMap: ju.Map[_,_] =>
      rawLevelsMap.foldLeft[List[(AdShowLevel, Int)]] (Nil) {
        case (mapAcc, (aslStr, count)) =>
          val k: AdShowLevel = AdShowLevels.withName(aslStr.toString)
          val v = count match {
            case n: java.lang.Number => n.intValue()
          }
          k -> v :: mapAcc
      }.toMap
  }

  def empty = ShowLevelLimits()

  /**
   * Десериализатор значения.
   * @param raw Выхлоп jackson'a.
   * @return Десериализованный экземпляр класса.
   */
  def deserialize(raw: Any): ShowLevelLimits = {
    raw match {
      case rawMpsl: ju.Map[_,_] =>
        def _s(fn: String): LvlMap_t = {
          Option( rawMpsl.get(fn) )
            .fold[LvlMap_t](Map.empty)(deserializeLevelsMap)
        }
        ShowLevelLimits(
          out = _s(OUT_ESFN)
        )

      case null => // Do nothing
        empty
    }
  }


  /** Опционально отрендерить карту полей в поле в play.json в аккамулятор. */
  private def maybeRenderLevelsMapPlayJson(name: String, levelsMap: LvlMap_t, acc: FieldsJsonAcc): FieldsJsonAcc = {
    if (levelsMap.nonEmpty) {
      val mapElements = levelsMap.foldLeft[FieldsJsonAcc] (Nil) {
        case (facc, (sl, max))  =>  sl.toString -> JsNumber(max) :: facc
      }
      name -> JsObject(mapElements) :: acc
    } else {
      acc
    }
  }

  // Хелперы для класса-компаньона.

  /** Определить макс.число карточек на указанном уровне с помощью указанной карты.
    * @param lvl Уровень.
    * @param levelsMap Карта уровней.
    * @return Неотрицательное целое. Если уровень запрещён, то будет 0.
    */
  def maxAtLevel(lvl: AdShowLevel, levelsMap: LvlMap_t): Int = {
    levelsMap.getOrElse(lvl, 0)
  }

  /**
   * Определить, возможно ли вообще что-либо постить/принимать на указанном уровне отображения.
   * @param lvl Уровень.
   * @param levelsMap Карта уровней.
   * @return true - значит карта допускает работу на этом уровне.
   */
  def canAtLevel(lvl: AdShowLevel, levelsMap: LvlMap_t): Boolean = {
    levelsMap.get(lvl).exists(isPossibleLevel)
  }

  private def isPossibleLevel(max: Int) = max > 0

  private def sls4render(sls: LvlMap_t) = {
    sls.toSeq
      .sortBy(_._1.visualPrio)
      .map {
        case (sl, slMax)  =>  sl -> (isPossibleLevel(slMax), slMax)
      }
  }
}

import ShowLevelLimits._
import io.suggest.model.sc.common.AdShowLevel
import play.api.libs.json.JsObject

/**
 * Данные по допустимым уровням отображения для входящих и исходящих публикаций.
 * @param out Карта уровней для исходящих публикаций.
 */
case class ShowLevelLimits(
  out: ShowLevelLimits.LvlMap_t = Map.empty
) {

  def toPlayJson: JsObject = {
    val acc = maybeRenderLevelsMapPlayJson(OUT_ESFN, out, Nil)
    JsObject(acc)
  }

  def isEmpty = out.isEmpty

  def canOutAtLevel(lvl: AdShowLevel) = canAtLevel(lvl, out)
  def maxOutAtLevel(lvl: AdShowLevel) = maxAtLevel(lvl, out)

  // Для рендера галочек нужна модифицированная карта.
  def out4render = sls4render(out)
}


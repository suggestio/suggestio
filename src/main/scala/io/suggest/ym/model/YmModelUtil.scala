package io.suggest.ym.model

import org.elasticsearch.action.update.UpdateRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.14 11:50
 * Description: Тут хранятся разные мелкие куски для разных моделей.
 */

/** Максимально-ленивая функция обновление уровней. Используется для уровней в MMartAd и MShop. */
object YmModelUtil {

  /**
   * Включить/выключить один из разрешенных уровней отображения в сохранённом магазине с помощью скрипта.
   * Этот метод предназначен для изменения только одного уровня и обновляет документ лениво и более точно.
   * @param updReq Исходный update-реквест.
   * @param levelFN Название поля документа, в котором хранится уровень.
   * @param level id уровня
   * @param isSet true - добавить уровень. false - удалить его.
   * @return Фьючерс для синхронизации.
   */
  def updateShowLevel(updReq: UpdateRequestBuilder, levelFN: String, level: AdShowLevel, isSet: Boolean): UpdateRequestBuilder = {
    // Максимально ленивый скрипт для апдейта списка уровней. Старается по возможности не изменять уже сохранённый документ.
    // Проверка на null по мотивам [[http://elasticsearch-users.115913.n3.nabble.com/partial-update-and-nested-type-td3959065.html]].
    val script = if (isSet) {
      """sls = ctx._source[fn]; if (sls == null) { ctx._source[fn] = sl } else { !sls.values.contains(sl) ? (ctx._source[fn] += sl) : (ctx.op = "none") }"""
    } else {
      """sls = ctx._source[fn]; if (sls == null) { ctx.op = "none" } else { sls.values.contains(sl) ? (ctx._source[fn] -= sl) : (ctx.op = "none") }"""
    }
    updReq
      .setScript(script)
      .addScriptParam("fn", levelFN)
      .addScriptParam("sl", level.toString)
  }

}

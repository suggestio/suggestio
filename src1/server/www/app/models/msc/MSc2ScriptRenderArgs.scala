package models.msc

import io.suggest.sc.MScApiVsn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.05.15 10:04
  * Description: Аргументы для рендера тегов скрипта js-выдачи в sc/siteTpl.
  *
  * 2016.sep.8: уже месяца полтора, как удалена v1-выдача. Сейчас выяснилось, что все параметры здесь - не нужны.
  * Все конечно удалены, но возможно надо удалить и модель,
  * если окажется она тоже не нужна после перепиливания геолокации выдачи?
  *
  * 2016.nov.9 Понадобилось значение API VSN в шаблоне [[views.html.sc.site.v2._scriptV2Tpl]].
  * Может унифицировать эту модель с SiteQsArgs?
  */

case class MSc2ScriptRenderArgs(
  apiVsn: MScApiVsn
)

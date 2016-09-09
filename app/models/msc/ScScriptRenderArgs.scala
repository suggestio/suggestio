package models.msc

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.05.15 10:04
  * Description: Аргументы для рендера тегов скрипта js-выдачи в sc/siteTpl.
  *
  * 2016.sep.8: уже месяца полтора, как удалена v1-выдача. Сейчас выяснилось, что все параметры здесь - не нужны.
  * Все конечно удалены, но возможно надо удалить и модель,
  * если окажется она тоже не нужна после перепиливания геолокации выдачи?
  */
trait IScScriptRenderArgs {

}


/** Дефолтовая реализация [[IScScriptRenderArgs]]. */
case class ScScriptRenderArgs(
)
  extends IScScriptRenderArgs

package io.suggest.sc.v.menu

import com.materialui.{MuiListItemClasses, MuiListItemProps}
import io.suggest.sc.v.styl.ScCssStatic

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.01.2021 16:49
  * Description: Утиль для рендера элементов меню.
  * m-ui v5.alpha23 имел визуальные косяки в стилях list item, применяемых в менюшке.
  * Тут исправляем это:
  */
final class MenuItemR {

  def MENU_LIST_ITEM_CSS_ROOT: String =
    ScCssStatic.Menu.Rows.mui5ListItem.htmlClass

  val MENU_LIST_ITEM_CSS: MuiListItemClasses = {
    new MuiListItemClasses {
      override val root = MENU_LIST_ITEM_CSS_ROOT
    }
  }

  val MENU_LIST_ITEM_PROPS: MuiListItemProps = {
    new MuiListItemProps {
      override val button = true
      override val disableGutters = DISABLE_GUTTERS
      override val classes = MENU_LIST_ITEM_CSS
    }
  }

  def DISABLE_GUTTERS = true

}

package io.suggest.i18n

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 21:41
  * Description: Пошаренная модель кодов сообщений.
  */
object MsgCodes {

  val `Subnodes` = "Subnodes"
  val `No.subnodes` = "No.subnodes"
  val `N.nodes` = "N.nodes"
  val `N.disabled` = "N.disabled"
  val `Is.enabled` = "Is.enabled"
  val `Name` = "Name"
  val `New.node` = "New.node"
  val `Delete` = "Delete"
  val `Deletion` = "Deletion"
  val `Yes.delete.it` = "Yes.delete.it"
  val `Are.you.sure` = "Are.you.sure"
  val `Comission.0.pct.for.sio` = "Comission.0.pct.for.sio"
  val `Cost` = "Cost"
  val `Show.details` = "Show.details"
  val `Information` = "Information"
  val `N.modules` = "N.modules"
  val `Send` = "Send"
  val `Send.request` = "Send.request"

  val `Add` = "Add"
  val `Error` = "Error"
  val `Something.gone.wrong` = "Something.gone.wrong"
  val `Identifier` = "Identifier"
  val `Cancel` = "Cancel"
  val `Save` = "Save"
  val `Saving` = "Saving"
  val `Saved` = "Saved"
  val `Change` = "Change"
  val `For.example.0` = "For.example.0"
  val `Close` = "Close"
  val `Create` = "Create"
  val `Edit` = "Edit"
  val `Node.id` = "Node.id"
  val `Please.wait` = "Please.wait"
  val `Please.try.again.later` = "Please.try.again.later"
  val `Example.id.0` = "Example.id.0"

  val `Adv.tariff` = "Adv.tariff"
  val `Inherited` = "Inherited"
  val `Set.manually` = "Set.manually"
  val `_per_.day` = "_per_.day"

  val `Tariff.rate.of.0` = "Tariff.rate.of.0"

  val `Server.request.in.progress.wait` = "Server.request.in.progress.wait"

  val `Beacon.name.example` = "Beacon.name.example"
  val `Type.new.name.for.beacon.0` = "Type.new.name.for.beacon.0"

  def `ofMonth.N.`(month_1_12: Int) = "ofMonth.N." + month_1_12
  def `DayOfWeek.N.`(dow_1_7: Int) = "DayOfWeek.N." + dow_1_7
  def `DayOfW.N.`(dow_1_7: Int) = "DayOfW.N." + dow_1_7
  def `dayOfW.N.`(dow_1_7: Int) = "dayOfW.N." + dow_1_7

  val `Yes` = "Yes"
  val `No` = "No"

  val `n.km._kilometers` = "n.km._kilometers"
  val `n.m._meters` = "n.m._meters"
  val `Radius` = "Radius"
  val `in.radius.of.0.from.1` = "in.radius.of.0.from.1"

  val `Adv.on.main.screen` = "Adv.on.main.screen"
  val `Coverage.area` = "Coverage.area"
  val `Ad.area.modules.count` = "Ad.area.modules.count"
  val `Tag` = "Tag"
  val `Date` = "Date"
  def `Date.suffixed`(suffix: String) = `Date` + "." + suffix
  val `Date.choosing` = "Date.choosing"
  val `Price` = "Price"
  val `Unable.to.initialize.map` = "Unable.to.initialize.map"
  val `Publish.node.on.adv.map` = "Publish.node.on.adv.map"
  val `Users.geo.location.capturing` = "Users.geo.location.capturing"
  val `You.can.place.adn.node.on.map.below` = "You.can.place.adn.node.on.map.below"
  val `Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle` = "Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle"

  val `Add.tags` = "Add.tags"

  val `Adv.on.map` = "Adv.on.map"
  val `Adv.on.map.hint` = "Adv.on.map.hint"

  val `Read.more` = "Read.more"
  val `Adv.geo.form.descr1` = "Adv.geo.form.descr1"
  val `User.located.inside.are.or.location.will.see.your.offer` = "User.located.inside.are.or.location.will.see.your.offer"
  val `Good.to.know.that` = "Good.to.know.that"
  val `Geo.circle.does.not.touch.any.nodes` = "Geo.circle.does.not.touch.any.nodes"
  val `Nodes.can.contain.subnodes.sublocations.routers.tvs.beacons` = "Nodes.can.contain.subnodes.sublocations.routers.tvs.beacons"
  val `Adv.geo.form.descr.price` = "Adv.geo.form.descr.price"

  val `Support.request` = "Support.request"
  val `Company.about` = "Company.about"
  val `Support.service` = "Support.service"
  val `Describe.problem` = "Describe.problem"
  val `Or.contact.us.via.email` = "Or.contact.us.via.email"

  val `OGRN` = "OGRN"
  val `Organization.name` = "Organization.name"
  val `_Cbca.name.full_` = "_Cbca.name.full_"
  val `INN` = "INN"
  val `KPP` = "KPP"
  val `Bank` = "Bank"
  val `_Cbca.bank.name_` = "_Cbca.bank.name_"
  val `Bank.BIK` = "Bank.BIK"
  val `Checking.account` = "Checking.account"
  val `Correspondent.account` = "Correspondent.account"
  val `Legal.address` = "Legal.address"
  val `_Cbca.address.legal_` = "_Cbca.address.legal_"
  val `Mail.address` = "Mail.address"
  val `_Cbca.address.mail_` = "_Cbca.address.mail_"

  val `Your.name` = "Your.name"
  val `Phone` = "Phone"

  val `Adv.for.free.without.moderation` = "Adv.for.free.without.moderation"

  val `Yandex.Kassa` = "Yandex.Kassa"
  val `Order.price` = "Order.price"
  val `Minimal.payment` = "Minimal.payment"
  val `Order.number` = "Order.number"
  val `Payment.method` = "Payment.method"
  val `Total.to.pay` = "Total.to.pay"
  val `Pay` = "Pay"
  val `Total.amount._money` = "Total.amount._money"
  val `_N` = "_N"
  val `Order.0.is.paid` = "Order.0.is.paid"
  val `Order.N` = "Order.N"
  val `We.received.payment.for.order.0` = "We.received.payment.for.order.0"
  val `Thank.you.for.using.our.service` = "Thank.you.for.using.our.service"
  val `Order.details` = "Order.details"

  val `Minimal.module` = "Minimal.module"
  val `scheme.left` = "scheme.left"
  val `Current.ad` = "Current.ad"
  val `Agreement.btw.CBCA.and.node.tariffs.for.year` = "Agreement.btw.CBCA.and.node.tariffs.for.year"

  val `Town` = "Town"
  val `Address` = "Address"
  val `Site` = "Site"
  val `Info.about.prods.and.svcs` = "Info.about.prods.and.svcs"
  val `Daily.people.traffic` = "Daily.people.traffic"
  val `Audience.descr` = "Audience.descr"

  val `Hello` = "Hello"
  val `Hello.0` = "Hello.0"
  val `Suggest.io` = "Suggest.io"
  val `iSuggest` = "iSuggest"
  val `All.rights.reserved` = "All.rights.reserved"
  val `day24h` = "day24h"

  val `Main.screen` = "Main.screen"
  val `GeoTag` = "GeoTag"
  val `_adv.Online.now` = "_adv.Online.now"

  val `Multi` = "Multi"

  val `Switch.node` = "Switch.node"

  val `Today` = "Today"

  val `locale.momentjs` = "locale.momentjs"
  val `Advertising.period` = "Advertising.period"


  val `__This.lang.name__` = "__This.lang.name__"

  // Эти коды без messages используются. Вполне вероятно, что в будущем у них появится локализация по языкам.
  val `left` = "left"
  val `right` = "right"


  val `Map` = "Map"
  val `Tags` = "Tags"

  val `Activation.impossible` = "Activation.impossible"

  val `Upper.block` = "Upper.block"
  val `also.displayed.in.grid` = "also.displayed.in.grid"
  val `Description` = "Description"

  val `Height` = "Height"
  val `Width`  = "Width"

  val `Delete.block` = "Delete.block"
  val `What.to.add` = "What.to.add"
  val `Content` = "Content"
  val `Block` = "Block"
  val `Example.text` = "Example.text"
  val `Bg.color` = "Bg.color"
  val `Fg.color.of.sc.hint` = "Fg.color.of.sc.hint"
  val `Profile` = "Profile"

  val `File.is.not.a.picture` = "File.is.not.a.picture"
  val `Apply` = "Apply"
  val `Picture.editing` = "Picture.editing"
  val `Crop` = "Crop"

  val `Cannot.checksum.file` = "Cannot.checksum.file"

  val `Suggested.bg.colors` = "Suggested.bg.colors"

  val `Scale` = "Scale"

  /** Непереводится на разные языки, однако бывает нужно. */
  val `TODO` = "TODO"

  val `Preparing`                   = "Preparing"
  val `Uploading.file`              = "Uploading.file"

  val `Stretch.across`              = "Stretch.across"

  val `Main.block`                  = "Main.block"
  val `Main.blocks.are.used.to.display.ad.inside.the.grid` = "Main.blocks.are.used.to.display.ad.inside.the.grid"
  val `This.block.is.the.only.main` = "This.block.is.the.only.main"
  val `This.block.is.not.main`      = "This.block.is.not.main"
  val `Show`                        = "Show"
  val `No.main.blocks.First.block.is.main` = "No.main.blocks.First.block.is.main"
  val `Background` = "Background"
  val `Upload.file` = "Upload.file"

  val `No.tags.here` = "No.tags.here"
  val `No.tags.found.for.1.query` = "No.tags.found.for.1.query"

  val `Login.page.title` = "Login.page.title"
  val `Login.to.sio` = "Login.to.sio"
  val `Go.to.node.ads` = "Go.to.node.ads"
  val `Personal.cabinet` = "Personal.cabinet"
  val `Suggest.io.Project` = "Suggest.io.Project"
  val `Suggest.io._transcription` = "Suggest.io._transcription"
  val `About.sio.node.id` = "About.sio.node.id"

  val `Rotation` = "Rotation"

  val `Ad.cards` = "Ad.cards"
  val `You.can.look.on.ads.in` = "You.can.look.on.ads.in"
  val `_ad.showcase.link` = "_ad.showcase.link"

  val `Create.ad` = "Create.ad"
  val `Show._ad` = "Show._ad"
  val `ad.Manage` = "ad.Manage"
  val `Show.ad.opened` = "Show.ad.opened"

  val `invalid_url` = "invalid_url"
  val `Welcome.screen` = "Welcome.screen"
  val `Welcome.bg.hint` = "Welcome.bg.hint"
  val `Node.photos` = "Node.photos"

  val `Bluetooth` = "Bluetooth"
  val `On` = "On"
  val `Off` = "Off"
  def onOff(isOn: Boolean): String =
    if (isOn) `On` else `Off`

  val `Clear` = "Clear"
  val `Search.start.typing` = "Search.start.typing"


  val `Cart` = "Cart"
  val `_order.Items` = "_order.Items"
  val `Delete.all` = "Delete.all"
  val `Node` = "Node"
  val `Your.cart.is.empty` = "Your.cart.is.empty"
  val `N.selected` = "N.selected"
  val `Total` = "Total"
  val `Reload` = "Reload"
  val `Go.to.payment.page` = "Go.to.payment.page"

  val `Bill.id` = "Bill.id"
  val `Bill.details` = "Bill.details"
  val `Sum` = "Sum"
  val `No.transactions.found` = "No.transactions.found"
  val `Payment.for.order.N` = "Payment.for.order.N"
  val `Transactions` = "Transactions"
  val `All` = "All"

  val `Nothing.found` = "Nothing.found"
  val `Nothing.to.moderate` = "Nothing.to.moderate"
  val `No.incoming.adv.requests` = "No.incoming.adv.requests"

  val `Lost.node` = "Lost.node"
  val `Approve` = "Approve"
  val `Refuse` = "Refuse"
  val `Reason` = "Reason"

  val `Next.node` = "Next.node"
  val `Previous.node` = "Previous.node"
  val `To.beginning` = "To.beginning"
  val `To.end` = "To.end"
  val `Moderation` = "Moderation"
  val `Moderation.needed` = "Moderation.needed"

  val `Moderate.all.nodes` = "Moderate.all.nodes"
  val `Moderate.requests.from.all.nodes` = "Moderate.requests.from.all.nodes"
  val `Go.to.moderation` = "Go.to.moderation"
  val `Nodes.management` = "Nodes.management"
  val `On.the.map` = "On.the.map"
  val `Does.not.adv.on.map` = "Does.not.adv.on.map"
  val `In.cart` = "In.cart"
  val `Adv.req.sent` = "Adv.req.sent"
  val `Will.be.adv` = "Will.be.adv"
  val `Adv.req.refused` = "Adv.req.refused"
  val `To.adv.on.map` = "To.adv.on.map"

  val `Layer`  = "Layer"
  val `Layers` = "Layers"
  val `Uppermost` = "Uppermost"
  val `Downmost` = "Downmost"
  val `Above` = "Above"
  val `Below` = "Below"
  val `Approved._adv` = "Approved._adv"
  val `Geolocation` = "Geolocation"

  val `Go.into` = "Go.into"
  val `Location.changed` = "Location.changed"
  val `Go.back` = "Go.back"
  val `Go.back.to.0` = "Go.back.to.0"

  val `Text.shadow` = "Text.shadow"
  val `Color` = "Color"
  val `Shadow.color` = "Shadow.color"

  val `No.ads.found.node.id` = "No.ads.found.node.id"

  val `GPS` = "GPS"
  val `0.uses.geoloc.to.find.ads` = "0.uses.geoloc.to.find.ads"
  val `0.uses.bt.to.find.ads.indoor` = "0.uses.bt.to.find.ads.indoor"
  val `Allow.0` = "Allow.0"
  val `Later` = "Later"
  val `You.can.enable.0.later.on.left.panel` = "You.can.enable.0.later.on.left.panel"
  val `Next` = "Next"
  val `Settings.done.0.ready.for.using` = "Settings.done.0.ready.for.using"
  val `_to.Finish` = "_to.Finish"

  val `GovServices.ESIA` = "GovServices.ESIA"

}

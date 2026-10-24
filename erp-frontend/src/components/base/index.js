import BaseButton from './BaseButton.vue'
import BasePageHeader from './BasePageHeader.vue'
import BaseTable from './BaseTable.vue'
import BaseDialog from './BaseDialog.vue'
import BaseStatCard from './BaseStatCard.vue'
import BaseStatusBadge from './BaseStatusBadge.vue'

export {
  BaseButton,
  BasePageHeader,
  BaseTable,
  BaseDialog,
  BaseStatCard,
  BaseStatusBadge
}

export default {
  install(app) {
    app.component('BaseButton', BaseButton)
    app.component('BasePageHeader', BasePageHeader)
    app.component('BaseTable', BaseTable)
    app.component('BaseDialog', BaseDialog)
    app.component('BaseStatCard', BaseStatCard)
    app.component('BaseStatusBadge', BaseStatusBadge)
  }
}


package dev.ujhhgtg.wekit.features.items.contacts

import android.view.MenuItem
import androidx.activity.ComponentActivity
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.features.api.net.models.protobuf.NearbyFriendProto
import dev.ujhhgtg.wekit.features.api.net.models.protobuf.WeProto
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.utils.reflection.int
import java.util.LinkedList

object AutoAddNearbyFriends : ClickableFeature(), IResolveDex {

    override val technicalId = "自动添加附近的人"
    override val nameRes = R.string.feature_auto_add_nearby_friends_name
    override val categoryIds = listOf(FeatureCategoryIds.CONTACTS_GROUPS)
    override val descriptionRes = R.string.feature_auto_add_nearby_friends_description

    // 8.0.79 移除了附近的人资料页直播浮层菜单: "NearbyPersonUIC" 日志标签与
    // "showLiveBottomSheet create menu." 日志均不再存在, 无等价结构可探测。
    // 该结构跨支持版本存在性不同 (8.0.65–8.0.78 存在, 8.0.79 起缺失), 按约定
    // 设 allowFailure: 旧版本正常注入菜单项, 新版本预期失败、功能降级为不可用,
    // 不影响其他 feature。
    private val methodCreateMenu by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("NearbyPersonUIC", "showLiveBottomSheet create menu.")
        }
    }

    private val methodMenuOnClick by dexMethod {
        matcher {
            usingEqStrings("com.tencent.mm.plugin.nearby.ui.NearbySayHiListUI")
            name = "onMMMenuItemSelected"
        }
    }

    override fun onEnable() {
        methodCreateMenu.hookBefore {
            args[0]!!.reflekt().firstMethod {
                parameters(int, CharSequence::class)
            }.invoke(6, localizedContactsString(R.string.contacts_auto_add_nearby_menu))
        }

        methodMenuOnClick.hookBefore {
            val menuItem = args[0] as MenuItem
            val itemId = menuItem.itemId
            if (itemId != 6) return@hookBefore

            val controller = thisObject!!.reflekt().firstField().get()!!
            val friends = controller.reflekt().firstField {
                type = List::class
            }.get()!! as LinkedList<*>

            val friendProtos = friends.map {
                WeProto.decode<NearbyFriendProto>(
                    it.reflekt().invokeMethod("toByteArray", superclass = true) as ByteArray
                )
            }

            result = null
        }
    }

    override fun onClick(context: ComponentActivity) {

    }
}

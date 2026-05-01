package org.gang

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

class MobileAgentFunctions(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) {

    /**
     * 1-1. 앱을 켜는 함수
     * 패키지명(예: "com.kakao.talk", "com.android.chrome")을 입력받아 해당 앱을 실행합니다.
     */
    fun startApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                // 에이전트가 백그라운드나 다른 컨텍스트에서 앱을 켤 수 있도록 플래그 추가
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 1-2. 앱을 끄는 함수
     * 안드로이드 권한 제약상 일반 앱이 타 앱을 완전히 강제 종료(Force Stop)할 수는 없으므로,
     * 백그라운드 프로세스를 종료하는 방식으로 메모리에서 내립니다.
     */
    fun stopApp(packageName: String) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 2. 앱의 UI 노드를 가져오는 함수
     * 에이전트가 현재 화면을 인식할 수 있도록 최상위(Root) UI 노드를 가져옵니다.
     */
    fun getUiNodes(): AccessibilityNodeInfo? {
        return accessibilityService.rootInActiveWindow
    }

    /**
     * 3. UI 노드 중 하나에 접근하여 클릭하는 함수
     * 특정 노드 객체를 받아 클릭 이벤트를 수행합니다.
     */
    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        var targetNode: AccessibilityNodeInfo? = node

        // 선택한 노드 자체가 클릭 불가능한 뷰(예: 단순 텍스트뷰, 이미지뷰)일 경우,
        // 클릭 액션을 받아줄 수 있는 부모 레이아웃이나 버튼이 나올 때까지 위로 탐색합니다.
        while (targetNode != null) {
            if (targetNode.isClickable) {
                val isClicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return isClicked
            }
            targetNode = targetNode.parent
        }
        return false
    }

    /**
     * 4. 클릭하여서 글을 복사 붙여넣기(입력)하는 함수
     * 특정 텍스트 입력창 노드에 포커스를 주고 원하는 텍스트를 입력합니다.
     */
    fun clickAndPasteText(node: AccessibilityNodeInfo?, textToInput: String): Boolean {
        if (node == null) return false

        // 1단계: 먼저 해당 노드에 포커스를 맞추기 위해 클릭 액션을 수행합니다.
        clickNode(node)

        // 2단계: 텍스트를 설정(입력)하는 액션에 필요한 인자를 번들에 담습니다.
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            textToInput
        )

        // 3단계: 텍스트 입력 액션을 실행합니다.
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    /**
     * (추가 유틸리티) 텍스트를 검색해서 클릭하고 바로 입력하는 통합 함수
     * 에이전트가 노드를 직접 찾기 번거로울 때 View ID를 기반으로 바로 입력할 수 있습니다.
     */
    fun findAndPasteTextByViewId(viewId: String, textToInput: String): Boolean {
        val rootNode = getUiNodes() ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)

        if (!nodes.isNullOrEmpty()) {
            val targetNode = nodes[0]
            val success = clickAndPasteText(targetNode, textToInput)

            // 메모리 최적화를 위해 탐색한 노드들을 재활용(recycle) 처리합니다.
            for (n in nodes) {
                n.recycle()
            }
            return success
        }
        return false
    }
}
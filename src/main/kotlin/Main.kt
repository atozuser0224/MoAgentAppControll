package org.gang

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class MobileAgentFunctions() {

    // 에이전트 시스템에서 초기화 시 주입해줘야 하는 정적 변수들
    companion object {
        var staticContext: Context? = null
        var staticService: AccessibilityService? = null

        /**
         * 에이전트 앱 측에서 DEX 로드 후 이 함수를 호출하여 초기화해야 합니다.
         */
        fun initialize(context: Context, service: AccessibilityService) {
            staticContext = context
            staticService = service
        }
    }

    /**
     * 1. 앱 실행
     * 입력: 패키지명 (String)
     * 출력: 성공 여부 "true" 또는 "false" (String)
     */
    fun startApp(packageName: String): String {
        val context = staticContext ?: return "Error: Context is null"
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                "true"
            } else {
                "false"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * 2. 앱 종료
     * 입력: 패키지명 (String)
     * 출력: 결과 메시지 (String)
     */
    fun stopApp(packageName: String): String {
        val context = staticContext ?: return "Error: Context is null"
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
            "Success"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * 3. 현재 화면의 모든 UI 텍스트 가져오기
     * 출력: 화면 내 모든 텍스트 요소를 쉼표로 구분한 문자열 (String)
     */
    fun getAllScreenTexts(): String {
        val rootNode = staticService?.rootInActiveWindow ?: return "Error: Root node null"
        val texts = mutableListOf<String>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            node.text?.let { texts.add(it.toString()) }
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(rootNode)
        return texts.joinToString(", ")
    }

    /**
     * 4. 특정 텍스트를 가진 요소 클릭
     * 입력: 클릭할 텍스트 (String)
     * 출력: 결과 메시지 (String)
     */
    fun clickByText(targetText: String): String {
        val rootNode = staticService?.rootInActiveWindow ?: return "Error: Root node null"
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)

        if (nodes.isNullOrEmpty()) return "Error: Text '$targetText' not found"

        for (node in nodes) {
            var temp: AccessibilityNodeInfo? = node
            while (temp != null) {
                if (temp.isClickable) {
                    val success = temp.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return if (success) "Success" else "Failed to perform click"
                }
                temp = temp.parent
            }
        }
        return "Error: No clickable node found for '$targetText'"
    }

    /**
     * 5. 특정 텍스트 입력창을 찾아 글자 입력
     * 입력: "ViewID|입력할텍스트" (String) - 구분자 '|' 사용
     * 출력: 결과 메시지 (String)
     */
    fun findAndInputText(data: String): String {
        val parts = data.split("|")
        if (parts.size < 2) return "Error: Invalid input format. Use 'viewId|text'"

        val viewId = parts[0]
        val textToInput = parts[1]

        val rootNode = staticService?.rootInActiveWindow ?: return "Error: Root node null"
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)

        if (nodes.isNullOrEmpty()) return "Error: View ID '$viewId' not found"

        val node = nodes[0]
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK) // 포커스

        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToInput)
        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

        return if (success) "Success" else "Failed to input text"
    }
}
# StackSSH 鎰忓浘璇嗗埆澧炲己鏂规 鈥?鎶€鏈璁℃枃妗?

> 鍩轰簬 StackSSH 涓婁笅鏂囪蹇嗕笌鎰忓浘璇嗗埆鏋舵瀯锛岄€傞厤 StackSSH 鐨?Spring Boot DDD + Google ADK 鎶€鏈爤銆?

---

## 涓€銆佽璁¤儗鏅笌鐩爣

### 1.1 鐜扮姸闂

StackSSH 褰撳墠瀛樺湪浠ヤ笅鑳藉姏缂哄彛锛?

| 缁村害 | 鐜扮姸 | 闂 |
|------|------|------|
| **System Prompt** | YAML 闈欐€?`instruction` 瀛楁 | 鏃犳硶鍦ㄨ繍琛屾椂娉ㄥ叆鐜淇℃伅銆佸巻鍙蹭笂涓嬫枃銆佺敤鎴锋剰鍥剧瓑鍔ㄦ€佸唴瀹?|
| **娑堟伅鍘嗗彶绠＄悊** | ADK `InMemoryRunner` 鍐呭瓨绠＄悊锛屾棤瑁佸壀 | 瀵硅瘽杞澧炲姞鍚?context 鎸佺画澧為暱锛屽彲鑳借秴鍑烘ā鍨?context window |
| **鎰忓浘璇嗗埆** | 鏃?| Agent 鏃犳硶鎻愬墠鎰熺煡鐢ㄦ埛鎰忓浘锛屾棤娉曚富鍔ㄥ噯澶囩浉鍏充笂涓嬫枃 |
| **鎰忓浘澧炲己** | 鏃?| 鐢ㄦ埛杈撳叆涓殑鏈嶅姟鍚嶃€佹枃浠惰矾寰勩€侀敊璇爜绛変俊鍙锋湭琚彁鍙栧埄鐢?|
| **浼氳瘽鎸佷箙鍖?* | 浠呭唴瀛橈紝閲嶅惎涓㈠け | 瀵硅瘽鍘嗗彶鏃犳硶璺ㄩ噸鍚繚鐣?|
| **涓婁笅鏂囨劅鐭?* | ThreadLocal 缁堢浼氳瘽缁戝畾 | 浠呮湁缁堢缁戝畾锛岀己涔忕粓绔姸鎬併€佸懡浠ゅ巻鍙茬瓑鐜鎰熺煡 |

### 1.2 璁捐鐩爣

1. **鍔ㄦ€?Prompt 鏋勫缓**锛歋ystem Prompt 浠?闈欐€?YAML"鍗囩骇涓?杩愯鏃跺姩鎬佺粍瑁?
2. **涓婁笅鏂囪蹇嗙鐞?*锛氬疄鐜?Provider-Reducer 绠￠亾锛岃В鍐?context 瓒呴檺闂
3. **鎰忓浘璇嗗埆绯荤粺**锛氫袱灞傚垎绫诲櫒閾撅紙瑙勫垯 + LLM锛夛紝閫傞厤 SSH 杩愮淮鍦烘櫙
4. **鎰忓浘澧炲己**锛氫俊鍙锋彁鍙?鈫?鏈嶅姟鍣ㄤ笂涓嬫枃鎼滅储 鈫?Prompt 娉ㄥ叆
5. **浼氳瘽鎸佷箙鍖?*锛歁ySQL 鎸佷箙鍖?+ Redis 缂撳瓨锛屾湇鍔￠噸鍚笉涓㈠け

### 1.3 StackSSH 璁捐鑼冨紡鍙傝€?

StackSSH 鐨勬牳蹇冭璁℃€濇兂锛?

- **Provider-Reducer 绠￠亾妯″紡**锛氫笂涓嬫枃鏀堕泦锛圥rovider锛変笌娑堟伅瑁佸壀锛圧educer锛夎В鑰?
- **涓夊眰鎰忓浘鍒嗙被鍣ㄩ摼**锛氳鍒欙紙蹇絾绮楋級鈫?妯″瀷锛堜腑绛夛級鈫?LLM锛堟參浣嗗噯锛夛紝閫愮骇鍗囩骇
- **"淇″彿鎻愬彇 鈫?浠ｇ爜鎼滅储 鈫?LLM 鍐崇瓥"澧炲己鑼冨紡**锛氬叧閿瘝鍙仛淇℃伅鎻愬彇锛屼笉鍋氬喅绛?
- **閲岀▼纰戠郴缁?*锛氱嫭绔嬩簬娑堟伅瑁佸壀鐨勫叧閿簨浠惰蹇?

---

## 浜屻€佹€讳綋鏋舵瀯

### 2.1 鏋舵瀯鎬昏

```
鐢ㄦ埛杈撳叆 "nginx 502浜嗭紝甯垜鎺掓煡"
    鈹?
    鈻?
[AgentServiceController.chatStream()]
    鈹?
    鈻?
[AIAgentReActServiceCase.chatStream()]
    鈹?
    鈹溾攢 鈶?IIntentService.classify()                    鎰忓浘璇嗗埆
    鈹?    鈹溾攢 RuleIntentClassifier  鈫?DIAGNOSE (0.7)
    鈹?    鈹斺攢 LLMIntentClassifier   鈫?DIAGNOSE (0.95)
    鈹?
    鈹溾攢 鈶?IIntentEnhancerService.enhance()             鎰忓浘澧炲己
    鈹?    鈹溾攢 SignalExtractor  鈫?{services: ["nginx"], errors: ["502"]}
    鈹?    鈹斺攢 ContextSearch    鈫?{nginx_status: "failed", logs: "..."}
    鈹?
    鈹溾攢 鈶?IChatContextService.buildContext()           涓婁笅鏂囩鐞?
    鈹?    鈹溾攢 TerminalStateProvider  鈫?{os: "Ubuntu 22.04", user: "root"}
    鈹?    鈹溾攢 MilestoneProvider      鈫?[{type: ERROR, content: "..."}]
    鈹?    鈹溾攢 ToolResultProvider     鈫?{summary: "宸插畨瑁?nginx 1.24"}
    鈹?    鈹斺攢 HybridReducer          鈫?瑁佸壀鍒?token 棰勭畻鍐?
    鈹?
    鈹溾攢 鈶?IPromptService.buildEnrichedMessage()         鍔ㄦ€?Prompt (棰嗗煙鏈嶅姟)
    鈹?    鈹溾攢 TerminalState閲囬泦 (ISshTerminalService)
    鈹?    鈹溾攢 MilestoneTracker (璁板綍涓庤幏鍙栧叧閿簨浠?
    鈹?    鈹斺攢 DynamicPromptBuilder (鍩虹 instruction + 鐜淇℃伅 + 鎰忓浘鍒嗘瀽 + 涓婁笅鏂?
    鈹?
    鈹斺攢 鈶?AiCallNode 鈫?runner.runAsync()               ADK 璋冪敤
          鈹?
          鈻?
        LLM 杩斿洖 鈫?宸ュ叿璋冪敤 鈫?缁撴灉 鈫?promptService.detectAndRecordMilestone() 鈫?娴佸紡杈撳嚭
```

### 2.2 鏂板妯″潡鐩綍缁撴瀯

```
StackSSH-server-domain/src/main/java/cn/stackssh/domain/agent/
鈹溾攢鈹€ service/
鈹?  鈹溾攢鈹€ IChatContextService.java              涓婁笅鏂囩鐞嗛鍩熸湇鍔℃帴鍙?
鈹?  鈹溾攢鈹€ IIntentService.java                   鎰忓浘璇嗗埆棰嗗煙鏈嶅姟鎺ュ彛
鈹?  鈹溾攢鈹€ IIntentEnhancerService.java           鎰忓浘澧炲己棰嗗煙鏈嶅姟鎺ュ彛
鈹?  鈹溾攢鈹€ IPromptService.java                   鎻愮ず璇嶆瀯寤洪鍩熸湇鍔℃帴鍙?
鈹?  鈹溾攢鈹€ armory/                               鏅鸿兘浣撹閰嶏紙google adk锛?
鈹?  鈹溾攢鈹€ context/                              涓婁笅鏂囪蹇嗘湇鍔″疄鐜板寘
鈹?  鈹?  鈹溾攢鈹€ ChatContextService.java           棰嗗煙鏈嶅姟瀹炵幇
鈹?  鈹?  鈹溾攢鈹€ provider/
鈹?  鈹?  鈹?  鈹溾攢鈹€ ContextProvider.java          Provider 鎺ュ彛
鈹?  鈹?  鈹?  鈹溾攢鈹€ TerminalStateProvider.java    缁堢鐘舵€侊紙OS銆佺敤鎴枫€佺洰褰曪級
鈹?  鈹?  鈹?  鈹溾攢鈹€ TaskProvider.java             褰撳墠浠诲姟
鈹?  鈹?  鈹?  鈹溾攢鈹€ MilestoneProvider.java        閲岀▼纰戜簨浠?
鈹?  鈹?  鈹?  鈹斺攢鈹€ ToolResultProvider.java       宸ュ叿缁撴灉鎽樿
鈹?  鈹?  鈹斺攢鈹€ reducer/
鈹?  鈹?      鈹溾攢鈹€ MessageReducer.java           Reducer 鎺ュ彛
鈹?  鈹?      鈹溾攢鈹€ PriorityReducer.java          浼樺厛绾ц鍓?
鈹?  鈹?      鈹溾攢鈹€ SlidingWindowReducer.java     婊戝姩绐楀彛瑁佸壀
鈹?  鈹?      鈹斺攢鈹€ HybridReducer.java            娣峰悎瑁佸壀锛堥粯璁わ級
鈹?  鈹溾攢鈹€ intent/                               鎰忓浘璇嗗埆鏈嶅姟瀹炵幇鍖?
鈹?  鈹?  鈹溾攢鈹€ IntentService.java                棰嗗煙鏈嶅姟瀹炵幇
鈹?  鈹?  鈹溾攢鈹€ ContextTracker.java               瀵硅瘽涓婁笅鏂囪拷韪櫒 (鍐呴儴缁勪欢)
鈹?  鈹?  鈹斺攢鈹€ classifier/
鈹?  鈹?      鈹溾攢鈹€ IntentClassifier.java         鍒嗙被鍣ㄦ帴鍙?
鈹?  鈹?      鈹溾攢鈹€ RuleIntentClassifier.java     绗?灞傦細瑙勫垯鍒嗙被
鈹?  鈹?      鈹斺攢鈹€ LLMIntentClassifier.java      绗?灞傦細LLM 鍒嗙被
鈹?  鈹溾攢鈹€ enhance/                              鎰忓浘澧炲己鏈嶅姟瀹炵幇鍖?
鈹?  鈹?  鈹溾攢鈹€ IntentEnhancerService.java        棰嗗煙鏈嶅姟瀹炵幇
鈹?  鈹?  鈹斺攢鈹€ processor/
鈹?  鈹?      鈹溾攢鈹€ SignalExtractor.java          淇″彿鎻愬彇 (鍐呴儴缁勪欢)
鈹?  鈹?      鈹斺攢鈹€ ContextSearch.java            鏈嶅姟鍣ㄤ笂涓嬫枃鎼滅储 (鍐呴儴缁勪欢)
鈹?  鈹溾攢鈹€ prompt/                               鎻愮ず璇嶆瀯寤烘湇鍔″疄鐜板寘
鈹?  鈹?  鈹溾攢鈹€ PromptService.java                棰嗗煙鏈嶅姟瀹炵幇
鈹?  鈹?  鈹斺攢鈹€ dynamic/
鈹?  鈹?      鈹溾攢鈹€ DynamicPromptBuilder.java     鍔ㄦ€?Prompt 缁勮鍣?(鍐呴儴缁勪欢)
鈹?  鈹?      鈹斺攢鈹€ MilestoneTracker.java         閲岀▼纰戣拷韪櫒 (鍐呴儴缁勪欢)
鈹溾攢鈹€ model/
鈹?  鈹溾攢鈹€ valobj/
鈹?  鈹?  鈹溾攢鈹€ prompt/
鈹?  鈹?  鈹?  鈹溾攢鈹€ PromptContextVO.java          Prompt 涓婁笅鏂囧€煎璞?
鈹?  鈹?  鈹?  鈹斺攢鈹€ MilestoneVO.java              閲岀▼纰?
鈹?  鈹?  鈹溾攢鈹€ IntentResult.java                 鎰忓浘璇嗗埆缁撴灉
鈹?  鈹?  鈹溾攢鈹€ ExtractedSignals.java             鎻愬彇鐨勪俊鍙?
鈹?  鈹?  鈹溾攢鈹€ SearchContext.java                鎼滅储涓婁笅鏂?
鈹?  鈹?  鈹斺攢鈹€ ConversationContext.java          瀵硅瘽涓婁笅鏂?
鈹?  鈹斺攢鈹€ entity/
鈹?      鈹斺攢鈹€ ChatMessageEntity.java            瀵硅瘽娑堟伅瀹炰綋
鈹斺攢鈹€ adaper/
    鈹斺攢鈹€ IChatHistoryRepository.java              瀵硅瘽鍘嗗彶鎸佷箙鍖栫綉鍏虫帴鍙?
```

---

## 涓夈€丳hase 1锛氬姩鎬?Prompt 鏋勫缓

### 3.1 璁捐璇存槑

褰撳墠 System Prompt 瀹屽叏鏉ヨ嚜 YAML 鐨?`instruction` 闈欐€佹枃鏈紝鏃犳硶鍦ㄨ繍琛屾椂娉ㄥ叆鐜淇℃伅銆佸巻鍙蹭笂涓嬫枃绛夊姩鎬佸唴瀹广€傛湰闃舵寮曞叆 `IPromptService` 棰嗗煙鏈嶅姟锛屽湪璋冪敤 LLM 鍓嶅姩鎬佺粍瑁呭畬鏁?Prompt銆備负绗﹀悎 DDD 瑙勮寖锛孋ase 灞傦紙`AiCallNode`锛変粎渚濊禆 `IPromptService` 鎺ュ彛锛岃€屼笉鐩存帴渚濊禆 `DynamicPromptBuilder`銆乣MilestoneTracker` 绛夊唴閮ㄧ粍浠躲€?

### 3.2 棰嗗煙妯″瀷 (Value Objects)

```java
package cn.stackssh.domain.agent.model.valobj.prompt;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class PromptContextVO {
    private String serverInfo;
    private String osInfo;
    private String currentUser;
    private String currentDirectory;

    private List<String> recentCommands;
    private List<MilestoneVO> milestoneVOS;

    // 鍚庣画 Phase 鎵╁睍
    // private IntentResult intentResult;
    // private Map<String, String> serviceStatus;
}
```

### 3.3 棰嗗煙鏈嶅姟 (Domain Service)

#### IPromptService 鎺ュ彛

鏆撮湶缁?Case 灞傜殑缁熶竴闂ㄩ潰锛?

```java
package cn.stackssh.domain.agent.service;

public interface IPromptService {
    void detectAndRecordMilestone(String sessionId, String role, String content);
    String buildEnrichedMessage(String userMessage, String sessionId, String terminalSessionId, List<String> recentCommands);
    void clearMilestones(String sessionId);
}
```

#### PromptService 瀹炵幇

缁勫悎鍐呴儴缁勪欢锛?

```java
package cn.stackssh.domain.agent.service.prompt;

@Service
public class PromptService implements IPromptService {
    @Resource private DynamicPromptBuilder dynamicPromptBuilder;
    @Resource private MilestoneTracker milestoneTracker;
    @Resource private ISshTerminalService sshTerminalService;

    @Override
    public String buildEnrichedMessage(String userMessage, String sessionId, String terminalSessionId, List<String> recentCommands) {
        // 1. 浠?SSH 缁堢閲囬泦鐜淇℃伅
        // 2. 浠?milestoneTracker 鑾峰彇浜嬩欢
        // 3. 鏋勫缓 PromptContextVO
        // 4. 璋冪敤 dynamicPromptBuilder.buildMessagePrefix() 鐢熸垚鍓嶇紑
        // 5. 鎷兼帴杩斿洖
    }
    
    // ... 鍏朵粬鏂规硶濮旀墭缁?tracker
}
```

### 3.4 鍔ㄦ€佺粍瑁呭櫒 (DynamicPromptBuilder)

```java
package cn.stackssh.domain.agent.service.prompt.dynamic;

@Component
public class DynamicPromptBuilder {
    /**
     * 灏嗗姩鎬佷笂涓嬫枃鏋勫缓涓虹敤鎴锋秷鎭墠缂€锛堟敞鍏ュ埌鐢ㄦ埛娑堟伅涓級
     * 閫傜敤浜?ADK 鏃犳硶鐩存帴鍦ㄨ繍琛屾椂淇敼 system instruction 鐨勫満鏅?
     */
    public String buildMessagePrefix(PromptContextVO ctx) {
        if (ctx == null) return "";
        StringBuilder sb = new StringBuilder();
        
        // 鎷兼帴 [绯荤粺鐜]
        // 鎷兼帴 [鏈€杩戞墽琛岀殑鍛戒护]
        // 鎷兼帴 [鍏抽敭浜嬩欢] (Milestones)
        
        return sb.toString();
    }
}
```

### 3.5 鏀归€?AiCallNode (Case 灞?

鍦?`AiCallNode` 涓紝浠呮敞鍏?`IPromptService`锛?

```java
// AiCallNode.java 鏀归€犵偣
@Resource
private IPromptService promptService;

private String buildEnrichedMessage(String userMessage, DynamicContext dynamicContext) {
    // 璁板綍鐢ㄦ埛娑堟伅鐨勯噷绋嬬
    promptService.detectAndRecordMilestone(dynamicContext.getSessionId(), "user", userMessage);

    // 濮旀墭棰嗗煙鏈嶅姟鏋勫缓瀵屽寲娑堟伅
    return promptService.buildEnrichedMessage(
            userMessage,
            dynamicContext.getSessionId(),
            dynamicContext.getTerminalSessionId(),
            dynamicContext.getRecentCommands()
    );
}

// 鍦ㄥ伐鍏锋墽琛岀粨鏋滃洖璋冨锛?
promptService.detectAndRecordMilestone(dynamicContext.getSessionId(), "tool", resultContent);
```

---

## 鍥涖€丳hase 2锛氫笂涓嬫枃璁板繂绠＄悊

### 4.1 璁捐璇存槑

閲囩敤 StackSSH 鐨?**Provider-Reducer 绠￠亾妯″紡**锛屽皢涓婁笅鏂囨敹闆嗕笌娑堟伅瑁佸壀瑙ｈ€︺€侾rovider 璐熻矗鏀堕泦鍚勭淮搴︿笂涓嬫枃锛孯educer 璐熻矗鍦?token 棰勭畻鍐呰鍓秷鎭€?

### 4.2 ContextProvider 鎺ュ彛

```java
package cn.stackssh.domain.agent.service.context.provider;

import java.util.Map;

public interface ContextProvider {
    String getName();
    int getOrder();
    boolean enabled();
    Map<String, Object> provide(String sessionId, String userId);
}
```

### 4.3 鍥涗釜 Provider 瀹炵幇

#### TerminalStateProvider锛坥rder=10锛?

鎻愪緵褰撳墠缁堢鐨勭郴缁熺幆澧冧俊鎭細

```java
@Component
public class TerminalStateProvider implements ContextProvider {
    @Resource
    private ISshTerminalService sshTerminalService;

    @Override public String getName() { return "terminal-state"; }
    @Override public int getOrder() { return 10; }
    @Override public boolean enabled() { return true; }

    @Override
    public Map<String, Object> provide(String sessionId, String userId) {
        Map<String, Object> result = new HashMap<>();
        String osInfo = safeExec(sessionId, "uname -srm");
        String user   = safeExec(sessionId, "whoami");
        String pwd    = safeExec(sessionId, "pwd");
        String uptime = safeExec(sessionId, "uptime -p 2>/dev/null || uptime");

        result.put("osInfo", osInfo);
        result.put("currentUser", user);
        result.put("currentDirectory", pwd);
        result.put("uptime", uptime);
        return result;
    }

    private String safeExec(String sessionId, String cmd) {
        try { return sshTerminalService.executeCommand(sessionId, cmd).trim(); }
        catch (Exception e) { return ""; }
    }
}
```

#### TaskProvider锛坥rder=20锛?

鎻愬彇褰撳墠瀵硅瘽涓殑浠诲姟鎻忚堪锛?

```java
@Component
public class TaskProvider implements ContextProvider {
    @Override public String getName() { return "task"; }
    @Override public int getOrder() { return 20; }
    @Override public boolean enabled() { return true; }

    @Override
    public Map<String, Object> provide(String sessionId, String userId) {
        Map<String, Object> result = new HashMap<>();
        // 浠?DynamicContext.messageHistory 涓彁鍙栫涓€鏉＄敤鎴锋秷鎭綔涓轰换鍔℃弿杩?
        List<Map<String, Object>> history = messageHistoryCache.get(sessionId);
        if (history != null) {
            history.stream()
                .filter(m -> "user".equals(m.get("role")))
                .findFirst()
                .ifPresent(m -> result.put("taskDescription", m.get("content")));
        }
        return result;
    }
}
```

#### MilestoneProvider锛坥rder=30锛?

鎻愪緵涓嶅彈瑁佸壀褰卞搷鐨勫叧閿簨浠讹細

```java
@Component
public class MilestoneProvider implements ContextProvider {
    @Resource
    private MilestoneTracker milestoneTracker;

    @Override public String getName() { return "milestoneVO"; }
    @Override public int getOrder() { return 30; }
    @Override public boolean enabled() { return true; }

    @Override
    public Map<String, Object> provide(String sessionId, String userId) {
        Map<String, Object> result = new HashMap<>();
        List<MilestoneVO> milestoneVOS = milestoneTracker.getRecent(sessionId, 10);
        result.put("milestoneVOS", milestoneVOS);
        return result;
    }
}
```

#### ToolResultProvider锛坥rder=40锛?

宸ュ叿鎵ц缁撴灉鐨勬噿鎽樿绛栫暐锛?

```java
@Component
public class ToolResultProvider implements ContextProvider {
    private final Map<String, List<ToolResultEntry>> results = new ConcurrentHashMap<>();
    private final Map<String, String> summaryCache = new ConcurrentHashMap<>();

    @Override public String getName() { return "tool-result"; }
    @Override public int getOrder() { return 40; }
    @Override public boolean enabled() { return true; }

    @Override
    public Map<String, Object> provide(String sessionId, String userId) {
        Map<String, Object> result = new HashMap<>();
        List<ToolResultEntry> entries = results.getOrDefault(sessionId, Collections.emptyList());
        if (entries.isEmpty()) return result;

        // 鎳掓憳瑕侊細鏈夌紦瀛樼洿鎺ヨ繑鍥烇紝鍚﹀垯閲嶆柊鐢熸垚
        String summary = summaryCache.computeIfAbsent(sessionId, id -> generateSummary(entries));
        result.put("toolResultSummary", summary);
        return result;
    }

    public void pushResult(String sessionId, ToolResultEntry entry) {
        results.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(entry);
        summaryCache.remove(sessionId);  // 澶辨晥鎽樿缂撳瓨
    }

    private String generateSummary(List<ToolResultEntry> entries) {
        // 灏戦噺缁撴灉鐩存帴鎷兼帴锛屽ぇ閲忕粨鏋滄ā鏉垮寲鍘嬬缉
        if (entries.size() <= 5) {
            return entries.stream()
                .map(e -> e.getToolName() + ": " + truncate(e.getResult(), 100))
                .collect(Collectors.joining("\n"));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("鏈€杩戞墽琛屼簡 ").append(entries.size()).append(" 涓伐鍏疯皟鐢?\n");
        // 鍙彇鏈€杩?5 鏉¤缁?+ 鎬荤粨
        List<ToolResultEntry> recent = entries.subList(entries.size() - 5, entries.size());
        for (ToolResultEntry e : recent) {
            sb.append("- ").append(e.getToolName()).append(": ")
              .append(truncate(e.getResult(), 80)).append("\n");
        }
        return sb.toString();
    }
}
```

### 4.4 MessageReducer 瑁佸壀绛栫暐

#### 鎺ュ彛瀹氫箟

```java
package cn.stackssh.domain.agent.service.context.reducer;

import java.util.List;
import java.util.Map;

public interface MessageReducer {
    List<Map<String, Object>> reduce(List<Map<String, Object>> messages, int tokenBudget);
}
```

#### PriorityReducer 鈥?浼樺厛绾ц鍓?

```java
@Component
public class PriorityReducer implements MessageReducer {

    @Override
    public List<Map<String, Object>> reduce(List<Map<String, Object>> messages, int tokenBudget) {
        // 涓烘瘡鏉℃秷鎭帹鏂紭鍏堢骇
        List<PrioritizedMessage> prioritized = messages.stream()
            .map(m -> new PrioritizedMessage(m, inferPriority(m)))
            .collect(Collectors.toList());

        // 鑷冲皯淇濈暀鏈€杩?2 鏉?
        int minKeep = Math.min(2, prioritized.size());
        List<PrioritizedMessage> kept = new ArrayList<>(prioritized.subList(
            prioritized.size() - minKeep, prioritized.size()));

        // 浠庝綆浼樺厛绾у紑濮嬩涪寮冿紝鐩村埌婊¤冻 token 棰勭畻
        int usedTokens = estimateTokens(kept);
        for (int i = prioritized.size() - minKeep - 1; i >= 0; i--) {
            PrioritizedMessage pm = prioritized.get(i);
            int msgTokens = estimateToken(pm.getMessage());
            if (usedTokens + msgTokens <= tokenBudget) {
                kept.add(0, pm);
                usedTokens += msgTokens;
            }
        }

        return kept.stream().map(PrioritizedMessage::getMessage).collect(Collectors.toList());
    }

    private Priority inferPriority(Map<String, Object> message) {
        String role = (String) message.get("role");
        String content = String.valueOf(message.get("content"));

        if ("tool".equals(role) && containsAny(content, "error", "failed", "exception", "permission denied")) {
            return Priority.CRITICAL;
        }
        if ("user".equals(role) && containsAny(content, "/", ".conf", ".yml", ".properties")) {
            return Priority.HIGH;
        }
        if ("system".equals(role)) {
            return Priority.HIGH;
        }
        if ("assistant".equals(role) && content.length() > 5000) {
            return Priority.LOW;
        }
        return Priority.MEDIUM;
    }

    enum Priority { CRITICAL, HIGH, MEDIUM, LOW }
}
```

#### SlidingWindowReducer 鈥?婊戝姩绐楀彛瑁佸壀

```java
@Component
public class SlidingWindowReducer implements MessageReducer {
    private static final int DEFAULT_WINDOW_SIZE = 20;

    @Override
    public List<Map<String, Object>> reduce(List<Map<String, Object>> messages, int tokenBudget) {
        List<Map<String, Object>> window = new ArrayList<>();
        int usedTokens = 0;

        // 浠庢柊鍒版棫閫愭潯娣诲姞锛岀洿鍒拌秴鍑?token 棰勭畻鎴栫獥鍙ｅぇ灏?
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> msg = messages.get(i);
            int msgTokens = estimateToken(msg);
            if (window.size() >= DEFAULT_WINDOW_SIZE || usedTokens + msgTokens > tokenBudget) break;
            window.add(0, msg);
            usedTokens += msgTokens;
        }
        return window;
    }
}
```

#### HybridReducer 鈥?娣峰悎瑁佸壀锛堥粯璁ょ瓥鐣ワ級

```java
@Component
public class HybridReducer implements MessageReducer {
    @Resource private PriorityReducer priorityReducer;
    @Resource private SlidingWindowReducer slidingReducer;

    @Override
    public List<Map<String, Object>> reduce(List<Map<String, Object>> messages, int tokenBudget) {
        Set<Integer> priorityKeep = indexSet(priorityReducer.reduce(messages, tokenBudget), messages);
        Set<Integer> slidingKeep  = indexSet(slidingReducer.reduce(messages, tokenBudget), messages);

        // 鍙栦氦闆?
        Set<Integer> keepIndices = new HashSet<>(priorityKeep);
        keepIndices.retainAll(slidingKeep);

        // 淇濊瘉鑷冲皯鏈夋渶杩?2 鏉?
        int minKeep = Math.min(2, messages.size());
        for (int i = messages.size() - minKeep; i < messages.size(); i++) {
            keepIndices.add(i);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if (keepIndices.contains(i)) result.add(messages.get(i));
        }
        return result;
    }

    private Set<Integer> indexSet(List<Map<String, Object>> subset, List<Map<String, Object>> all) {
        Set<Integer> indices = new HashSet<>();
        for (Map<String, Object> msg : subset) {
            int idx = all.indexOf(msg);
            if (idx >= 0) indices.add(idx);
        }
        return indices;
    }
}
```

### 4.5 MilestoneTracker 鈥?閲岀▼纰戠郴缁?

```java
@Component
public class MilestoneTracker {
    private static final int MAX_MILESTONES = 50;
    private final Map<String, LinkedList<MilestoneVO>> milestoneVOS = new ConcurrentHashMap<>();

    public void detectAndRecord(String sessionId, String role, String content) {
        MilestoneVO.Type type = null;

        if ("user".equals(role)) {
            if (matches(content, "涓嶅|涓嶆槸杩欐牱|鏀逛竴涓媩鎹釜鎬濊矾|鎹㈢鏂瑰紡|閿欎簡")) {
                type = MilestoneVO.Type.TASK_CHANGE;
            } else if (matches(content, "瀹屾垚浜唡鎼炲畾|缁撴潫|濂戒簡")) {
                type = MilestoneVO.Type.TASK_COMPLETE;
            } else if (matches(content, "涓嶈|鍋渱鍒?)) {
                type = MilestoneVO.Type.USER_CORRECTION;
            }
        }

        if ("tool".equals(role)) {
            if (matches(content, "(?i)error|failed|exception|permission denied|not found|refused")) {
                type = MilestoneVO.Type.ERROR;
            }
        }

        if (type != null) {
            push(sessionId, MilestoneVO.builder()
                .type(type)
                .content(truncate(content, 200))
                .timestamp(System.currentTimeMillis())
                .build());
        }
    }

    private void push(String sessionId, MilestoneVO milestoneVO) {
        LinkedList<MilestoneVO> list = milestoneVOS.computeIfAbsent(
            sessionId, k -> new LinkedList<>());
        synchronized (list) {
            list.addLast(milestoneVO);
            while (list.size() > MAX_MILESTONES) list.removeFirst();
        }
    }

    public List<MilestoneVO> getRecent(String sessionId, int limit) {
        LinkedList<MilestoneVO> list = milestoneVOS.getOrDefault(sessionId, new LinkedList<>());
        synchronized (list) {
            int from = Math.max(0, list.size() - limit);
            return new ArrayList<>(list.subList(from, list.size()));
        }
    }

    public void clear(String sessionId) {
        milestoneVOS.remove(sessionId);
    }

    private boolean matches(String content, String regex) {
        return content != null && content.matches(".*(" + regex + ").*");
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
```

### 4.6 IChatContextService 涓?ChatContextService 鈥?涓婁笅鏂囩鐞嗛鍩熸湇鍔?

涓虹鍚?DDD 瑙勮寖锛屾彁鍙栫粺涓€鐨勬帴鍙?`IChatContextService`锛屽苟鍦?`ChatContextService` 涓紪鎺?Provider 鍜?Reducer锛?

```java
package cn.stackssh.domain.agent.service;

public interface IChatContextService {
    PromptContextVO buildPromptContext(String sessionId, String userId, String terminalSessionId);
    List<Map<String, Object>> trimHistory(List<Map<String, Object>> history, int tokenBudget);
}
```

```java
package cn.stackssh.domain.agent.service.context;

import cn.stackssh.domain.agent.service.IChatContextService;
import cn.stackssh.domain.agent.service.IPromptService;
@Service
public class ChatContextService implements IChatContextService {
    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 8000;

    @Resource
    private List<ContextProvider> providers;
    @Resource
    private HybridReducer hybridReducer;
    @Resource
    private IPromptService promptService;
    @Resource
    private ISshTerminalService sshTerminalService;

    @PostConstruct
    public void init() {
        providers.sort(Comparator.comparingInt(ContextProvider::getOrder));
    }

    @Override
    public PromptContextVO buildPromptContext(String sessionId, String userId, String terminalSessionId) {
        // 鐢变簬 Lombok Builder 鐨勭壒鎬э紝鎴戜滑鐩存帴閫氳繃閾惧紡璋冪敤鎴栬€呬复鏃跺彉閲忓瓨鍌ㄦ瀯寤?
        Map<String, Object> finalCtx = new HashMap<>();

        for (ContextProvider provider : providers) {
            if (!provider.enabled()) continue;
            Map<String, Object> ctx = provider.provide(sessionId, userId);
            finalCtx.putAll(ctx);
        }

        return PromptContextVO.builder()
                .osInfo((String) finalCtx.get("osInfo"))
                .currentUser((String) finalCtx.get("currentUser"))
                .currentDirectory((String) finalCtx.get("currentDirectory"))
                .serverInfo((String) finalCtx.get("serverInfo"))
                .milestoneVOS((List<MilestoneVO>) finalCtx.get("milestoneVOS"))
                // 鍚庣画 Phase 鎵╁睍
                // .serviceStatus((Map<String, String>) finalCtx.get("serviceStatus"))
                // .fileContents((Map<String, String>) finalCtx.get("fileContents"))
                // .recentLogs((Map<String, String>) finalCtx.get("recentLogs"))
                .build();
    }

    @Override
    public List<Map<String, Object>> trimHistory(List<Map<String, Object>> history, int tokenBudget) {
        if (history == null || history.isEmpty()) return Collections.emptyList();
        return hybridReducer.reduce(history, tokenBudget > 0 ? tokenBudget : DEFAULT_MAX_CONTEXT_TOKENS);
    }
}
```

---

## 浜斻€丳hase 3锛氭剰鍥捐瘑鍒郴缁?

### 5.1 璁捐璇存槑

閲囩敤 **涓ゅ眰鍒嗙被鍣ㄩ摼**锛堣鍒?+ LLM锛夛紝閫傞厤鍚庣鏈嶅姟鍦烘櫙锛堢渷鍘?StackSSH 鍓嶇鐨?妯″瀷鍒嗙被鍣?灞傦紝鐩存帴瑙勫垯 + LLM 涓ゅ眰锛岄檷浣庡欢杩燂級銆?

### 5.2 鎰忓浘绫诲瀷瀹氫箟

```java
public enum IntentType {
    DIAGNOSE("璇婃柇闂"),
    CONFIGURE("閰嶇疆淇敼"),
    DEPLOY("閮ㄧ讲鎿嶄綔"),
    MONITOR("鐩戞帶鏌ョ湅"),
    SECURITY("瀹夊叏鐩稿叧"),
    BACKUP("澶囦唤鎭㈠"),
    EXECUTE("鐩存帴鎵ц"),
    EXPLAIN("瑙ｉ噴璇存槑"),
    SEARCH("鎼滅储鏌ユ壘"),
    CHAT("闂茶亰"),
    CONTINUE("缁х画"),
    UNKNOWN("鏈煡");

    private final String label;
    IntentType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

### 5.3 IntentResult 鍊煎璞?

```java
@Data
@Builder
public class IntentResult {
    private IntentType intent;
    private double confidence;
    private Map<String, String> entities;
    private String rawResponse;
}
```

### 5.4 IIntentService 涓?IntentService 鈥?鎰忓浘鍒嗙被棰嗗煙鏈嶅姟

鎻愪緵缁熶竴鐨勬剰鍥惧垎绫婚鍩熸湇鍔℃帴鍙ｏ紝闅愯棌鍐呴儴鐨勫垎绫诲櫒缂栨帓锛?

```java
package cn.stackssh.domain.agent.service;

public interface IIntentService {
    IntentResult classify(String sessionId, String userId, String message);
}
```

```java
package cn.stackssh.domain.agent.service.intent;

import cn.stackssh.domain.agent.service.IIntentService;

@Service
public class IntentService implements IIntentService {
    @Resource private RuleIntentClassifier ruleClassifier;
    @Resource private LLMIntentClassifier llmClassifier;
    @Resource private ContextTracker contextTracker;

    private final Cache<String, IntentResult> cache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    @Override
    public IntentResult classify(String sessionId, String userId, String message) {
        String cacheKey = sessionId + ":" + hashMessage(message);
        IntentResult cached = cache.getIfPresent(cacheKey);
        if (cached != null) return cached;

        ConversationContext context = contextTracker.getContext(sessionId);

        // 绗?灞傦細瑙勫垯鍒嗙被锛? 1ms锛?
        IntentResult ruleResult = ruleClassifier.classify(message, context);
        if (ruleResult.getConfidence() >= 0.8) {
            recordAndCache(sessionId, cacheKey, ruleResult);
            return ruleResult;
        }

        // 绗?灞傦細LLM 鍒嗙被锛?00-500ms锛?
        IntentResult llmResult = llmClassifier.classify(message, context);
        IntentResult finalResult = llmResult.getConfidence() >= 0.5 ? llmResult : ruleResult;

        recordAndCache(sessionId, cacheKey, finalResult);
        return finalResult;
    }

    private void recordAndCache(String sessionId, String cacheKey, IntentResult result) {
        contextTracker.updateContext(sessionId, result);
        cache.put(cacheKey, result);
    }

    private String hashMessage(String message) {
        return Integer.toHexString(message.hashCode());
    }
}
```

### 5.5 RuleIntentClassifier 鈥?瑙勫垯鍒嗙被鍣?

```java
@Component
public class RuleIntentClassifier implements IntentClassifier {

    private static final List<IntentRule> RULES = List.of(
        rule(IntentType.DIAGNOSE,
            List.of("鎸備簡", "瀹曟満", "down", "502", "503", "504", "OOM", "婊?, "杩囬珮", "寮傚父",
                    "鎶ラ敊", "鍛婅", "瓒呮椂", "timeout", "crash", "panic", "fatal"),
            List.of("涓轰粈涔?*(?:鎸倈鎶ラ敊|澶辫触|涓嶉€?", "鎺掓煡.*闂", "鍒嗘瀽.*鍘熷洜"),
            Map.of(List.of("淇", "fix", "瑙ｅ喅"), 0.1)),

        rule(IntentType.CONFIGURE,
            List.of("閰嶇疆", "config", "淇敼閰嶇疆", "鍙傛暟", "璋冩暣", "璁剧疆", "璋冧紭"),
            List.of("淇敼.*(?:conf|cfg|yml|properties|xml|json)", "璁剧疆.*鍙傛暟"),
            Map.of()),

        rule(IntentType.DEPLOY,
            List.of("閮ㄧ讲", "deploy", "鍙戝竷", "鍥炴粴", "rollback", "涓婄嚎", "鏇存柊鐗堟湰", "閲嶅惎鏈嶅姟"),
            List.of("(?:鍙戝竷|閮ㄧ讲).*鐗堟湰", "鍥炴粴.*鐗堟湰"),
            Map.of()),

        rule(IntentType.MONITOR,
            List.of("鏌ョ湅", "鐩戞帶", "鏃ュ織", "log", "cpu", "鍐呭瓨", "纾佺洏", "缃戠粶", "娴侀噺",
                    "璐熻浇", "load", "杩涚▼", "绔彛", "杩炴帴鏁?),
            List.of("(?:鐪媩鏌check).*(?:鐘舵€亅鎯呭喌|浣跨敤鐜?", "tail.*log"),
            Map.of()),

        rule(IntentType.SECURITY,
            List.of("闃茬伀澧?, "firewall", "iptables", "鏉冮檺", "permission", "ssh", "瀵嗛挜",
                    "璇佷功", "ssl", "tls", "瀹夊叏", "婕忔礊", "CVE"),
            List.of("(?:寮€鏀緗鍏抽棴).*绔彛", "閰嶇疆.*(?:ssl|璇佷功|瀵嗛挜)"),
            Map.of()),

        rule(IntentType.BACKUP,
            List.of("澶囦唤", "backup", "鎭㈠", "restore", "瀵煎嚭", "import", "杩佺Щ"),
            List.of("澶囦唤.*(?:鏁版嵁搴搢鏂囦欢|閰嶇疆)", "鎭㈠.*鏁版嵁"),
            Map.of()),

        rule(IntentType.EXPLAIN,
            List.of("浠€涔堟剰鎬?, "鎬庝箞鐞嗚В", "瑙ｉ噴", "璇存槑", "explain", "what is", "how to"),
            List.of("杩欎釜鍛戒护.*(?:鎰忔€潀浣滅敤|鐢ㄩ€?"),
            Map.of()),

        rule(IntentType.SEARCH,
            List.of("鎵?, "鎼滅储", "grep", "find", "locate", "鏌ユ壘", "鍝釜杩涚▼", "鍝釜鏂囦欢"),
            List.of("(?:鎵緗鎼滅储).*(?:鏂囦欢|杩涚▼|绔彛)"),
            Map.of())
    );

    @Override
    public IntentResult classify(String message, ConversationContext context) {
        String lowerMsg = message.toLowerCase();
        IntentResult best = IntentResult.builder()
            .intent(IntentType.UNKNOWN).confidence(0.0).entities(Map.of()).build();

        for (IntentRule rule : RULES) {
            double score = 0.0;

            // 鍏抽敭璇嶅尮閰嶏紙鏈€楂?0.6锛?
            long hits = rule.getKeywords().stream()
                .filter(lowerMsg::contains).count();
            score += Math.min(0.6, hits * 0.2);

            // 姝ｅ垯鍖归厤锛堥澶?+0.2锛?
            boolean patternHit = rule.getPatterns().stream()
                .anyMatch(p -> Pattern.matches(".*" + p + ".*", message));
            if (patternHit) score += 0.2;

            // 涓婁笅鏂囧姞鏉冿細鏈€杩戞剰鍥句竴鑷村垯 +0.1
            if (context.getRecentIntents().stream()
                .anyMatch(h -> h.getIntent() == rule.getIntent())) {
                score += 0.1;
            }

            score = Math.min(1.0, score);

            if (score > best.getConfidence()) {
                best = IntentResult.builder()
                    .intent(rule.getIntent())
                    .confidence(score)
                    .entities(extractEntities(message, rule.getIntent()))
                    .build();
            }
        }
        return best;
    }

    private Map<String, String> extractEntities(String message, IntentType intent) {
        Map<String, String> entities = new HashMap<>();
        // 鎻愬彇鏈嶅姟鍚?
        List<String> services = List.of("nginx", "redis", "mysql", "postgres", "docker",
            "kafka", "rabbitmq", "elasticsearch", "tomcat", "spring");
        services.stream().filter(message.toLowerCase()::contains)
            .forEach(svc -> entities.put("service", svc));
        return entities;
    }

    private static IntentRule rule(IntentType intent, List<String> keywords,
                                   List<String> patterns, Map<List<String>, Double> contextBoost) {
        IntentRule r = new IntentRule();
        r.setIntent(intent);
        r.setKeywords(keywords);
        r.setPatterns(patterns);
        r.setContextBoost(contextBoost);
        return r;
    }
}
```

### 5.6 LLMIntentClassifier 鈥?LLM 鍒嗙被鍣?

```java
@Component
public class LLMIntentClassifier implements IntentClassifier {
    @Resource
    private ChatModel chatModel;

    private static final String CLASSIFY_PROMPT_TEMPLATE = """
        浣犳槸涓€涓?SSH 杩愮淮鍦烘櫙鐨勬剰鍥捐瘑鍒郴缁熴€傚垎鏋愮敤鎴疯緭鍏ワ紝杩斿洖 JSON 鏍煎紡鐨勬剰鍥惧垎绫荤粨鏋溿€?
        
        ## 鎰忓浘绫诲瀷
        - DIAGNOSE: 璇婃柇闂锛堟湇鍔℃寕浜嗐€佹姤閿欍€佸紓甯告帓鏌ワ級
        - CONFIGURE: 閰嶇疆淇敼锛堟敼閰嶇疆鏂囦欢銆佽皟鍙傛暟锛?
        - DEPLOY: 閮ㄧ讲鎿嶄綔锛堥儴缃层€佸彂甯冦€佸洖婊氾級
        - MONITOR: 鐩戞帶鏌ョ湅锛堢湅鏃ュ織銆佹煡鐘舵€併€佺湅璧勬簮浣跨敤锛?
        - SECURITY: 瀹夊叏鐩稿叧锛堥槻鐏銆佹潈闄愩€佽瘉涔︼級
        - BACKUP: 澶囦唤鎭㈠锛堝浠芥暟鎹€佹仮澶嶆暟鎹級
        - EXECUTE: 鐩存帴鎵ц锛堝府鎴戣窇鏌愬懡浠わ級
        - EXPLAIN: 瑙ｉ噴璇存槑锛堣繖涓懡浠や粈涔堟剰鎬濓級
        - SEARCH: 鎼滅储鏌ユ壘锛堟壘鏂囦欢銆佹煡杩涚▼锛?
        - CHAT: 闂茶亰
        - CONTINUE: 缁х画涓婁竴涓换鍔?
        - UNKNOWN: 鏃犳硶鍒ゆ柇
        
        ## 杈撳嚭鏍煎紡锛堜粎杩斿洖 JSON锛屾棤鍏朵粬鍐呭锛?
        {"intent":"绫诲瀷","confidence":0.0-1.0,"entities":{"key":"value"}}
        
        ## 绀轰緥
        杈撳叆: "nginx 502浜嗭紝甯垜鐪嬬湅"
        杈撳嚭: {"intent":"DIAGNOSE","confidence":0.95,"entities":{"service":"nginx","error":"502"}}
        
        杈撳叆: "甯垜鏀逛笅 redis 鐨?maxmemory 閰嶇疆"
        杈撳嚭: {"intent":"CONFIGURE","confidence":0.9,"entities":{"service":"redis","config":"maxmemory"}}
        
        杈撳叆: "鐪嬩笅鏈嶅姟鍣ㄧ鐩樹娇鐢ㄦ儏鍐?
        杈撳嚭: {"intent":"MONITOR","confidence":0.9,"entities":{"resource":"disk"}}
        
        杈撳叆: "杩欎釜鍛戒护 awk '{print $1}' access.log 鏄粈涔堟剰鎬?
        杈撳嚭: {"intent":"EXPLAIN","confidence":0.95,"entities":{"command":"awk"}}
        
        ## 瀵硅瘽涓婁笅鏂?
        鏈€杩戞剰鍥? %s
        
        ## 鍒嗘瀽浠ヤ笅杈撳叆
        杈撳叆: "%s"
        杈撳嚭:
        """;

    @Override
    public IntentResult classify(String message, ConversationContext context) {
        String recentIntents = context.getRecentIntents().stream()
            .map(h -> h.getIntent().name())
            .collect(Collectors.joining(", "));

        String prompt = String.format(CLASSIFY_PROMPT_TEMPLATE,
            recentIntents.isEmpty() ? "鏃? : recentIntents, message);

        try {
            String response = chatModel.call(prompt).getResult().getOutput().getContent();
            return parseResponse(response);
        } catch (Exception e) {
            return IntentResult.builder()
                .intent(IntentType.UNKNOWN).confidence(0.0).entities(Map.of()).build();
        }
    }

    private IntentResult parseResponse(String response) {
        try {
            // 鎻愬彇 JSON 閮ㄥ垎
            String json = response.replaceAll("(?s).*?(\\{.*}).*", "$1");
            Map<String, Object> parsed = new ObjectMapper().readValue(json, Map.class);

            IntentType intent = IntentType.valueOf(
                String.valueOf(parsed.get("intent")).toUpperCase());
            double confidence = parsed.containsKey("confidence")
                ? Double.parseDouble(String.valueOf(parsed.get("confidence"))) : 0.5;
            Map<String, String> entities = parsed.containsKey("entities")
                ? (Map<String, String>) parsed.get("entities") : Map.of();

            return IntentResult.builder()
                .intent(intent).confidence(confidence)
                .entities(entities).rawResponse(response).build();
        } catch (Exception e) {
            return IntentResult.builder()
                .intent(IntentType.UNKNOWN).confidence(0.0)
                .entities(Map.of()).rawResponse(response).build();
        }
    }
}
```

### 5.7 ContextTracker 鈥?瀵硅瘽涓婁笅鏂囪拷韪櫒

```java
@Component
public class ContextTracker {
    private static final int WINDOW_SIZE = 10;
    private final Map<String, ConversationContext> contexts = new ConcurrentHashMap<>();

    public ConversationContext getContext(String sessionId) {
        return contexts.computeIfAbsent(sessionId, id -> ConversationContext.builder()
            .recentIntents(new LinkedList<>())
            .turnCount(0)
            .sessionStartTime(System.currentTimeMillis())
            .build());
    }

    public void updateContext(String sessionId, IntentResult result) {
        ConversationContext ctx = getContext(sessionId);
        ctx.getRecentIntents().addLast(IntentHistoryEntry.builder()
            .intent(result.getIntent())
            .confidence(result.getConfidence())
            .timestamp(System.currentTimeMillis())
            .build());
        if (ctx.getRecentIntents().size() > WINDOW_SIZE) {
            ctx.getRecentIntents().removeFirst();
        }
        ctx.setTurnCount(ctx.getTurnCount() + 1);
        ctx.setLastIntent(result.getIntent());
    }

    public void clear(String sessionId) {
        contexts.remove(sessionId);
    }
}
```

---

## 鍏€丳hase 4锛氭剰鍥惧寮?鈥?淇″彿鎻愬彇涓庝笂涓嬫枃娉ㄥ叆

### 6.1 璁捐璇存槑

浠庣敤鎴疯緭鍏ヤ腑鎻愬彇缁撴瀯鍖栦俊鍙凤紙鏈嶅姟鍚嶃€佹枃浠惰矾寰勩€侀敊璇爜绛夛級锛屽埌鐩爣鏈嶅姟鍣ㄤ笂鎼滅储鐩稿叧涓婁笅鏂囷紙鏈嶅姟鐘舵€併€侀厤缃枃浠躲€佹棩蹇楋級锛屾敞鍏ュ埌 Prompt 涓緟鍔?LLM 鍐崇瓥銆?

鏍稿績鎬濇兂锛?*鍏抽敭璇嶅彧鍋氫俊鎭彁鍙栵紝涓嶅仛鍐崇瓥** 鈥?SignalExtractor 璐熻矗鎻愬彇淇″彿锛孋ontextSearch 璐熻矗鏌ユ壘涓婁笅鏂囷紝LLM 缁熶竴鍋氱悊瑙ｅ喅绛栥€?

### 6.2 ExtractedSignals 鍊煎璞?

```java
@Data
@Builder
public class ExtractedSignals {
    private List<String> serverHosts;
    private List<String> filePaths;
    private List<String> serviceNames;
    private List<String> commandHints;
    private List<String> errorPatterns;
    private List<String> logKeywords;
}
```

### 6.3 SignalExtractor 鈥?淇″彿鎻愬彇鍣?

```java
@Component
public class SignalExtractor {

    private static final List<String> KNOWN_SERVICES = List.of(
        "nginx", "apache", "httpd", "redis", "mysql", "mariadb", "postgres", "postgresql",
        "mongodb", "kafka", "rabbitmq", "docker", "containerd", "kubernetes", "kubelet",
        "jenkins", "gitlab", "elasticsearch", "kibana", "logstash", "prometheus", "grafana",
        "zookeeper", "etcd", "consul", "nacos", "tomcat", "spring", "node", "php-fpm",
        "sshd", "firewalld", "iptables", "crond", "rsyslogd"
    );

    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(
        "(?:/[\\w.-]+)+\\.(?:conf|cfg|yml|yaml|properties|xml|json|log|sh|service|ini|cnf)");

    private static final Pattern IP_PATTERN = Pattern.compile(
        "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    public ExtractedSignals extract(String message) {
        String lower = message.toLowerCase();
        return ExtractedSignals.builder()
            .serverHosts(extract(IP_PATTERN, message))
            .filePaths(extract(FILE_PATH_PATTERN, message))
            .serviceNames(KNOWN_SERVICES.stream().filter(lower::contains).collect(Collectors.toList()))
            .commandHints(extractCommandHints(lower))
            .errorPatterns(extractErrorPatterns(message))
            .logKeywords(extractLogKeywords(message))
            .build();
    }

    private List<String> extract(Pattern pattern, String text) {
        List<String> results = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) results.add(matcher.group());
        return results;
    }

    private List<String> extractCommandHints(String lower) {
        List<String> hints = new ArrayList<>();
        List<String> cmds = List.of("systemctl", "service", "journalctl", "tail", "grep",
            "awk", "sed", "find", "curl", "wget", "ping", "telnet", "netstat", "ss",
            "top", "htop", "free", "df", "du", "ps", "kill", "lsof", "iptables",
            "docker", "kubectl", "apt", "yum", "rpm");
        cmds.stream().filter(lower::contains).forEach(hints::add);
        return hints;
    }

    private List<String> extractErrorPatterns(String message) {
        List<String> patterns = new ArrayList<>();
        List<Pattern> errorPatterns = List.of(
            Pattern.compile("(?i)(?:HTTP\\s*)?5\\d{2}"),
            Pattern.compile("(?i)\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+"),
            Pattern.compile("(?i)(?:error|exception|fatal|panic|oom|segfault)[\\s:]?.{0,50}"),
            Pattern.compile("(?i)connection\\s+(?:refused|timed\\s*out|reset)"),
            Pattern.compile("(?i)permission\\s+denied"),
            Pattern.compile("(?i)no\\s+such\\s+file"),
            Pattern.compile("(?i)disk\\s+(?:full|space)"),
            Pattern.compile("(?i)port\\s+\\d+")
        );
        for (Pattern p : errorPatterns) {
            Matcher m = p.matcher(message);
            while (m.find()) patterns.add(m.group());
        }
        return patterns;
    }

    private List<String> extractLogKeywords(String message) {
        List<String> keywords = new ArrayList<>();
        List<String> logLevels = List.of("error", "warn", "warning", "fatal", "critical",
            "exception", "timeout", "refused", "denied", "failed", "oom");
        String lower = message.toLowerCase();
        logLevels.stream().filter(lower::contains).forEach(keywords::add);
        return keywords;
    }
}
```

### 6.4 ContextSearch 鈥?鏈嶅姟鍣ㄤ笂涓嬫枃鎼滅储

```java
@Component
public class ContextSearch {
    @Resource
    private ISshTerminalService sshTerminalService;

    public SearchContext searchBySignals(String terminalSessionId, ExtractedSignals signals) {
        if (terminalSessionId == null || terminalSessionId.isEmpty()) {
            return SearchContext.builder().build();
        }

        SearchContext.SearchContextBuilder builder = SearchContext.builder();

        // 1. 鏌ヨ鏈嶅姟鐘舵€?
        if (!signals.getServiceNames().isEmpty()) {
            Map<String, String> statusMap = new LinkedHashMap<>();
            for (String svc : signals.getServiceNames()) {
                String status = safeExec(terminalSessionId,
                    "systemctl is-active " + svc + " 2>/dev/null || " +
                    "service " + svc + " status 2>&1 | head -3");
                statusMap.put(svc, status);
            }
            builder.serviceStatus(statusMap);
        }

        // 2. 璇诲彇鐩稿叧閰嶇疆鏂囦欢锛堝彇鍓?50 琛岋級
        if (!signals.getFilePaths().isEmpty()) {
            Map<String, String> contentMap = new LinkedHashMap<>();
            for (String path : signals.getFilePaths()) {
                String content = safeExec(terminalSessionId,
                    "head -50 " + path + " 2>/dev/null");
                if (!content.isEmpty() && !content.contains("No such file")) {
                    contentMap.put(path, content);
                }
            }
            builder.fileContents(contentMap);
        }

        // 3. 鎼滅储鏈€杩戞棩蹇?
        if (!signals.getErrorPatterns().isEmpty() || !signals.getLogKeywords().isEmpty()) {
            Map<String, String> logMap = new LinkedHashMap<>();
            for (String svc : signals.getServiceNames()) {
                String logs = safeExec(terminalSessionId,
                    "journalctl -u " + svc + " --no-pager -n 30 --since '1 hour ago' 2>/dev/null || " +
                    "tail -30 /var/log/" + svc + "/*.log 2>/dev/null || " +
                    "tail -30 /var/log/" + svc + ".log 2>/dev/null");
                if (!logs.isEmpty()) logMap.put(svc, logs);
            }
            // 濡傛灉娌℃湁鎸囧畾鏈嶅姟浣嗘湁閿欒妯″紡锛屾悳绱㈢郴缁熸棩蹇?
            if (logMap.isEmpty() && !signals.getErrorPatterns().isEmpty()) {
                String sysLogs = safeExec(terminalSessionId,
                    "dmesg --time-format iso -T 2>/dev/null | tail -30 || dmesg | tail -30");
                if (!sysLogs.isEmpty()) logMap.put("system", sysLogs);
            }
            builder.recentLogs(logMap);
        }

        return builder.build();
    }

    private String safeExec(String sessionId, String cmd) {
        try {
            String result = sshTerminalService.executeCommand(sessionId, cmd);
            return result != null ? result.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
```

### 6.5 IIntentEnhancerService 涓?IntentEnhancerService 鈥?鎰忓浘澧炲己棰嗗煙鏈嶅姟

閫氳繃鏆撮湶鎺ュ彛渚?Case 灞傝皟鐢細

```java
package cn.stackssh.domain.agent.service;

public interface IIntentEnhancerService {
    SearchContext enhance(String terminalSessionId, String userMessage);
}
```

```java
package cn.stackssh.domain.agent.service.enhance;

import cn.stackssh.domain.agent.service.IIntentEnhancerService;

@Service
public class IntentEnhancerService implements IIntentEnhancerService {
    @Resource private SignalExtractor signalExtractor;
    @Resource private ContextSearch contextSearch;

    @Override
    public SearchContext enhance(String terminalSessionId, String userMessage) {
        // Step 1: 淇″彿鎻愬彇
        ExtractedSignals signals = signalExtractor.extract(userMessage);

        boolean hasSignals = !signals.getServiceNames().isEmpty()
            || !signals.getFilePaths().isEmpty()
            || !signals.getErrorPatterns().isEmpty()
            || !signals.getServerHosts().isEmpty();

        if (!hasSignals) {
            return SearchContext.builder().build();
        }

        // Step 2: 鏍规嵁淇″彿鏌ユ壘鏈嶅姟鍣ㄤ笂涓嬫枃
        return contextSearch.searchBySignals(terminalSessionId, signals);
    }
}
```

### 6.6 SearchContext 鍊煎璞?

```java
@Data
@Builder
public class SearchContext {
    @Builder.Default
    private Map<String, String> serviceStatus = Map.of();
    @Builder.Default
    private Map<String, String> fileContents = Map.of();
    @Builder.Default
    private Map<String, String> recentLogs = Map.of();
}
```

---

## 涓冦€丳hase 5锛氫細璇濇寔涔呭寲

### 7.1 鏁版嵁搴撹〃璁捐

```sql
-- 浼氳瘽鍏冩暟鎹?
CREATE TABLE `chat_session` (
    `id`            VARCHAR(64)     NOT NULL COMMENT '浼氳瘽ID',
    `agent_id`      VARCHAR(64)     NOT NULL COMMENT '鏅鸿兘浣揑D',
    `user_id`       VARCHAR(64)     NOT NULL COMMENT '鐢ㄦ埛ID',
    `title`         VARCHAR(200)    DEFAULT NULL COMMENT '浼氳瘽鏍囬',
    `created_at`    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `message_count` INT             DEFAULT 0 COMMENT '娑堟伅鏁伴噺',
    PRIMARY KEY (`id`),
    INDEX `idx_user_agent` (`user_id`, `agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瀵硅瘽浼氳瘽';

-- 瀵硅瘽娑堟伅
CREATE TABLE `chat_message` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `session_id`    VARCHAR(64)     NOT NULL COMMENT '浼氳瘽ID',
    `role`          VARCHAR(20)     NOT NULL COMMENT '瑙掕壊: user/assistant/tool/system',
    `content`       TEXT            COMMENT '娑堟伅鍐呭',
    `tool_name`     VARCHAR(100)    DEFAULT NULL COMMENT '宸ュ叿鍚嶇О',
    `tool_call_id`  VARCHAR(100)    DEFAULT NULL COMMENT '宸ュ叿璋冪敤ID',
    `priority`      VARCHAR(20)     DEFAULT 'MEDIUM' COMMENT '浼樺厛绾? CRITICAL/HIGH/MEDIUM/LOW',
    `token_count`   INT             DEFAULT 0 COMMENT '棰勪及 token 鏁?,
    `created_at`    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_time` (`session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瀵硅瘽娑堟伅';

-- 閲岀▼纰戜簨浠?
CREATE TABLE `chat_milestone` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `session_id`    VARCHAR(64)     NOT NULL COMMENT '浼氳瘽ID',
    `type`          VARCHAR(30)     NOT NULL COMMENT '绫诲瀷: TASK_CHANGE/ERROR/DECISION/...',
    `content`       TEXT            COMMENT '鍐呭鎽樿',
    `created_at`    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_time` (`session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瀵硅瘽閲岀▼纰?;
```

### 7.2 缃戝叧鎺ュ彛

```java
public interface IChatHistoryGateway {
    void saveMessage(String sessionId, ChatMessageEntity message);
    List<ChatMessageEntity> getRecentMessages(String sessionId, int limit);
    List<ChatMessageEntity> getMessagesWithBudget(String sessionId, int tokenBudget);
    void saveMilestone(String sessionId, MilestoneVO milestoneVO);
    List<MilestoneVO> getRecentMilestones(String sessionId, int limit);
}
```

---

## 鍏€佸疄鏂借鍒?

### 8.1 鍒嗛樁娈典紭鍏堢骇

| 闃舵 | 鍐呭 | 渚濊禆 | 棰勪及宸ヤ綔閲?|
|------|------|------|-----------|
| **Phase 1** | 鍔ㄦ€?Prompt 鏋勫缓 | 鏃?| 2 澶?|
| **Phase 2** | 涓婁笅鏂囪蹇嗙鐞?| Phase 1 | 4 澶?|
| **Phase 5** | 浼氳瘽鎸佷箙鍖?| Phase 2 | 3 澶?|
| **Phase 3** | 鎰忓浘璇嗗埆绯荤粺 | Phase 1 | 3 澶?|
| **Phase 4** | 鎰忓浘澧炲己 | Phase 3 | 3 澶?|

### 8.2 寤鸿钀藉湴椤哄簭

```
Phase 1锛堝姩鎬?Prompt锛?
    鈫?
Phase 2锛堜笂涓嬫枃璁板繂锛?
    鈫?
Phase 5锛堜細璇濇寔涔呭寲锛?
    鈫?
Phase 3锛堟剰鍥捐瘑鍒級
    鈫?
Phase 4锛堟剰鍥惧寮猴級
```

鐞嗙敱锛?
1. Phase 1 鏀瑰姩鏈€灏忋€佹敹鐩婃渶鐩存帴锛屾槸鍚庣画鎵€鏈夊姛鑳界殑鍩虹
2. Phase 2 瑙ｅ喅鏍稿績鐥涚偣锛坈ontext 瓒呴檺锛夛紝Phase 5 鏄叾蹇呰琛ュ厖
3. Phase 3 + 4 鏄綋楠屽寮哄眰锛屽湪鍩虹绋冲畾鍚庡彔鍔?

### 8.3 鍏抽敭鏀归€犳枃浠舵竻鍗?

| 鏂囦欢 | 鏀归€犲唴瀹?|
|------|---------|
| `IPromptService.java` / `PromptService.java` | **銆怭hase 1銆?* 鎻愮ず璇嶄笌涓婁笅鏂囨瀯寤虹殑缁熶竴棰嗗煙鏈嶅姟 |
| `DynamicPromptBuilder.java` | 鍔ㄦ€佺粍瑁呮彁绀鸿瘝鐨勫簳灞傜粍浠?|
| `MilestoneTracker.java` | 閲岀▼纰戝叧閿簨浠剁殑妫€娴嬩笌瀛樺偍 |
| `IChatContextService.java` / `ChatContextService.java` | **銆怭hase 2銆?* 涓婁笅鏂囩鐞嗛鍩熸湇鍔★紝灏佽 Provider 鍜?Reducer 閫昏緫 |
| `IIntentService.java` / `IntentService.java` | **銆怭hase 3銆?* 鎰忓浘鍒嗙被棰嗗煙鏈嶅姟锛屽皝瑁呬袱灞傚垎绫诲櫒閫昏緫 |
| `IIntentEnhancerService.java` / `IntentEnhancerService.java` | **銆怭hase 4銆?* 鎰忓浘澧炲己棰嗗煙鏈嶅姟锛屽皝瑁呬俊鍙锋彁鍙栦笌鎼滅储 |
| `AiCallNode.java` | **銆怭hase 1銆?* 娉ㄥ叆 `IPromptService`锛屽疄鐜扮幆澧冧俊鎭拰鎰忓浘绛変笂涓嬫枃鐨勫姩鎬佹敞鍏?|
| `AIAgentReActServiceCase.java` | 鍦?chatStream 鍏ュ彛澶勮皟鐢ㄩ鍩熸湇鍔★紙濡?`IIntentService`銆乣IIntentEnhancerService`锛夎繘琛屾剰鍥捐瘑鍒拰澧炲己 |
| `DefaultReActFactory.java` | DynamicContext 鏂板鎰忓浘缁撴灉鍜屾悳绱笂涓嬫枃瀛楁 |
| `ChatService.java` | 闆嗘垚浼氳瘽鎸佷箙鍖?|
| `SshExecuteAdkTool.java` | 宸ュ叿缁撴灉鎺ㄩ€佸埌 MilestoneTracker 鍜?ToolResultProvider |
| `application-dev.yml` | 鏂板鎰忓浘璇嗗埆鍜屼笂涓嬫枃绠＄悊鐨勯厤缃紑鍏?|

---

## 涔濄€佷笌 StackSSH 璁捐鐨勫叧閿€傞厤宸紓

| 缁村害 | StackSSH | StackSSH 閫傞厤 |
|------|----------|-------------|
| 杩愯鐜 | Tauri 鍓嶇杩涚▼ | Spring Boot 鍚庣锛屽鐢ㄦ埛骞跺彂 |
| LLM 璋冪敤 | 鐩存帴 HTTP API | Spring AI 鈫?ADK 妗ユ帴 |
| 涓婁笅鏂囨潵婧?| 缂栬緫鍣ㄦ枃浠躲€佸厜鏍囥€乀ab | SSH 缁堢鐘舵€併€佸懡浠ゅ巻鍙层€佹湇鍔＄姸鎬?|
| 淇″彿鎻愬彇 | 浠ｇ爜鏂囦欢璺緞銆佺鍙峰悕 | 鏈嶅姟鍣ㄥ湴鍧€銆佹枃浠惰矾寰勩€佹湇鍔″悕銆侀敊璇爜 |
| 浼氳瘽瀛樺偍 | Tauri 鏂囦欢绯荤粺锛堟湰鍦?JSON锛?| MySQL + Redis锛堟湇鍔＄鎸佷箙鍖栵級 |
| 骞跺彂妯″瀷 | 鍗曠敤鎴峰崟杩涚▼ | 澶氱敤鎴峰绾跨▼锛圕oncurrentHashMap + Redis锛?|
| 鎰忓浘绫诲瀷 | code_edit / debug / refactor | DIAGNOSE / CONFIGURE / DEPLOY / MONITOR |
| Provider 閫傞厤 | FileProvider锛堢紪杈戝櫒鏂囦欢锛?| TerminalStateProvider锛堢粓绔姸鎬併€佺郴缁熶俊鎭級 |
| 鍒嗙被鍣ㄥ眰鏁?| 涓夊眰锛堣鍒欌啋妯″瀷鈫扡LM锛?| 涓ゅ眰锛堣鍒欌啋LLM锛夛紝鍚庣鐪佸幓涓棿灞傞檷浣庡欢杩?|
| 缂撳瓨绛栫暐 | 鍐呭瓨 Map | Caffeine 鏈湴缂撳瓨 + Redis 鍒嗗竷寮忕紦瀛?|


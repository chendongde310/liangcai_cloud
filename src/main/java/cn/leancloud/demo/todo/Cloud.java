package cn.leancloud.demo.todo;

import cn.leancloud.*;
import cn.leancloud.sms.AVSMS;
import cn.leancloud.sms.AVSMSOption;
import cn.leancloud.types.AVNull;
import cn.leancloud.utils.StringUtil;
import com.alibaba.fastjson.JSONArray;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

//lean deploy 上传
public class Cloud {
    /**
     * @param userId 用户ID
     * @return
     */
    @EngineFunction("moveJobs")
    public static boolean moveJobs( @EngineFunctionParam("phone") String phone, @EngineFunctionParam("userId") String userId,@EngineFunctionParam("userInfoId") String userInfoId) {

        AVQuery<AVObject> avQuery = new AVQuery<>("Job");
        avQuery.whereEqualTo("phone", phone);
        List<AVObject> jobs = avQuery.find();
        AVObject avUser = AVObject.createWithoutData("UserInfo",userInfoId);
        AVObject hr = new AVObject("HR");
        hr.put("gmPhone", phone);
        hr.put("name", "未认证企业");
        hr.put("state", "未认证");
        avUser.put("hr", hr);
        avUser.put("hrPower", "master");

        avUser.save();

        if (jobs.size() > 0) {
            AVACL acl = new AVACL();
            acl.setPublicReadAccess(true);
            acl.setWriteAccess(userId, true);
            for (AVObject job : jobs) {
                job.put("hr", hr);
                job.put("user", avUser);
                job.setACL(acl);
            }
            try {
                AVObject.saveAll(jobs);
            } catch (AVException e) {
                e.printStackTrace();
            }
            AVObject log = new AVObject("Log");
            log.put("content", "职位转移" + phone + "    userInfoId" + avUser.getObjectId());
            log.save();
            return true;
        }else {
            return false;
        }

    }


    @EngineFunction("verifyHR")
    public static String verifyHR(@EngineFunctionParam("userId") String userId, @EngineFunctionParam("HRId") String hrId) {
        AVObject hr = AVObject.createWithoutData("HR", hrId);
        AVObject avUser = AVObject.createWithoutData("UserInfo", userId);
        hr.put("state", "审核");
        hr.save();

        AVQuery<AVObject> avQuery = new AVQuery<>("Job");
        avQuery.whereEqualTo("user", avUser);
        avQuery.findInBackground().subscribe(new Observer<List<AVObject>>() {
            @Override
            public void onSubscribe(@NotNull Disposable disposable) {

            }

            @Override
            public void onNext(@NotNull List<AVObject> avObjects) {
                for (AVObject avObject : avObjects) {
                    avObject.put("hr", hr);
                }
                try {
                    AVObject.saveAll(avObjects);
                } catch (AVException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(@NotNull Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        return "";
    }


}

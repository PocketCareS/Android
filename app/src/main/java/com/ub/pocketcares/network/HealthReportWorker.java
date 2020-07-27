package com.ub.pocketcares.network;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ub.pocketcares.backend.DailyStatusDatabaseHelper;

import java.io.IOException;

public class HealthReportWorker extends Worker {
    private Context healthReportContext;

    public HealthReportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.healthReportContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        final Result[] workResult = new Result[1];
        workResult[0] = Result.success();
        Runnable dataUpload = () -> {
            final DailyStatusDatabaseHelper ddh = new DailyStatusDatabaseHelper(healthReportContext);
            HTTPHelper helper = new HTTPHelper();
            try {
                String token = ServerHelper.generateToken(healthReportContext, helper);
                String healthReport = ddh.getUploadData();
                if (healthReport != null) {
                    uploadDailySymptoms(helper, healthReport, token);
                }
            } catch (Exception e) {
                e.printStackTrace();
                workResult[0] = Result.retry();
            } finally {
                ddh.closeDB();
            }
        };
        new Thread(dataUpload).start();
        return workResult[0];
    }

    private void uploadDailySymptoms(HTTPHelper helper, String data, String token) throws IOException {
        if (data != null) {
            helper.postRequest(ServerHelper.SYMPTOMS_ENDPOINT, data, token);
        }
    }
}

/*
 * Copyright (c) 2016 CommonsWare, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.netcipher.hurl;

import android.app.ListActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongConnectionBuilder;
import sample.netcipher.hurl.model.Item;
import sample.netcipher.hurl.model.SOQuestions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;

public class MainActivity extends ListActivity implements
        StrongBuilder.Callback<HttpURLConnection> {
    static final String SO_URL =
            "https://api.stackexchange.com/2.1/questions?"
                    + "order=desc&sort=creation&site=stackoverflow&tagged=android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            StrongConnectionBuilder
                    .forMaxSecurity(this)
                    .withTorValidation()
                    .connectTo(SO_URL)
                    .build(this);
        } catch (Exception e) {
            Toast
                    .makeText(this, R.string.msg_crash, Toast.LENGTH_LONG)
                    .show();
            Log.e(getClass().getSimpleName(),
                    "Exception loading SO questions", e);
            finish();
        }
    }

    @Override
    public void onConnected(final HttpURLConnection conn) {
        new Thread() {
            @Override
            public void run() {
                try {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(in));

                    final SOQuestions result =
                            new Gson().fromJson(reader, SOQuestions.class);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setListAdapter(new ItemsAdapter(result.items));
                        }
                    });

                    reader.close();
                } catch (IOException e) {
                    onConnectionException(e);
                } finally {
                    conn.disconnect();
                }
            }
        }.start();
    }

    @Override
    public void onConnectionException(Exception e) {
        Toast
                .makeText(this, R.string.msg_crash, Toast.LENGTH_LONG)
                .show();
        Log.e(getClass().getSimpleName(),
                "Exception loading SO questions", e);
        finish();
    }

    @Override
    public void onTimeout() {
        Toast
                .makeText(this, R.string.msg_timeout, Toast.LENGTH_LONG)
                .show();
        finish();
    }

    @Override
    public void onInvalid() {
        Toast
                .makeText(this, R.string.msg_invalid, Toast.LENGTH_LONG)
                .show();
        finish();
    }

    class ItemsAdapter extends ArrayAdapter<Item> {
        ItemsAdapter(List<Item> items) {
            super(MainActivity.this,
                    android.R.layout.simple_list_item_1, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);
            TextView title = (TextView) row.findViewById(android.R.id.text1);

            title.setText(Html.fromHtml(getItem(position).title));

            return (row);
        }
    }
}

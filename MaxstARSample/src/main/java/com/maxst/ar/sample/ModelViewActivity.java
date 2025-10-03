package com.maxst.ar.sample;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModelViewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ModelAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_view);
        recyclerView = findViewById(R.id.activity_model_rcview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchModels("6549f08d6bec839a13ece0ae");
    }
    private void fetchModels(String machineId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ModelResponse> call = apiService.getModels(machineId);

        call.enqueue(new Callback<ModelResponse>() {
            @Override
            public void onResponse(Call<ModelResponse> call, Response<ModelResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ModelData> models = response.body().getData();
                    adapter = new ModelAdapter(models , ModelViewActivity.this);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(ModelViewActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelResponse> call, Throwable t) {
              //  Toast.makeText(ModelViewActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", t.getMessage(), t);
            }
        });
    }


    public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ViewHolder> {

        private List<ModelData> modelList;
        private Context context;

        public ModelAdapter(List<ModelData> modelList,Context context) {
            this.context = context;
            this.modelList = modelList;
        }

        public  class ViewHolder extends RecyclerView.ViewHolder {
            public TextView name, desc, url, createdAt;
            public CardView cardView;

            public ViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.name);
                desc = view.findViewById(R.id.desc);
                url = view.findViewById(R.id.url);
                createdAt = view.findViewById(R.id.createdAt);
                cardView = view.findViewById(R.id.cardView);
            }
        }

        @Override
        public ModelAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ModelData model = modelList.get(position);
            holder.name.setText(model.getName());
            holder.desc.setText(model.getDesc());
            holder.url.setText(model.getUrl());
            holder.createdAt.setText(model.getCreatedAt());

            holder.cardView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ModelActivity.class);
               // intent.putExtra("model_url", model.getUrl());
                intent.putExtra("model_data", model);
                context.startActivity(intent);
            });

        }

        @Override
        public int getItemCount() {
            return modelList.size();
        }
    }

}
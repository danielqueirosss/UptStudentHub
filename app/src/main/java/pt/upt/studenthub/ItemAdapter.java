package pt.upt.studenthub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Adapter da ListView: reutiliza linhas e referências às Views com o padrão ViewHolder. */
public final class ItemAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private List<MarineHour> items = Collections.emptyList();
    private Set<String> favorites = Collections.emptySet();
    private ExplorerSettings settings = ExplorerSettings.defaults();

    /** O Context pertence ao adapter do ecrã, nunca ao ViewModel. */
    public ItemAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    /** Atualiza os dados; devolve true apenas quando a lista mudou, para controlar a animação. */
    public boolean submitItems(List<MarineHour> newItems, ExplorerSettings newSettings) {
        boolean changed = !items.equals(newItems);
        boolean thresholdChanged = settings.waveThresholdMetres != newSettings.waveThresholdMetres;
        items = Collections.unmodifiableList(new ArrayList<>(newItems));
        settings = newSettings;
        if (changed || thresholdChanged) {
            notifyDataSetChanged();
        }
        return changed;
    }

    /** Atualiza as estrelas sem alterar a ordem ou a posição dos elementos. */
    public void submitFavorites(Set<String> keys) {
        if (!favorites.equals(keys)) {
            favorites = Collections.unmodifiableSet(new HashSet<>(keys));
            notifyDataSetChanged();
        }
    }

    /** Número de horas atualmente apresentadas. */
    @Override
    public int getCount() {
        return items.size();
    }

    /** Modelo da linha selecionada, usado para abrir o detalhe da praia e hora corretas. */
    @Override
    public MarineHour getItem(int position) {
        return items.get(position);
    }

    /** Identificador posicional; os favoritos usam MarineHour.key(), não este valor. */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /** Preenche todos os campos ao reutilizar uma linha, incluindo ícones e visibilidade. */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_marine_hour, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MarineHour item = getItem(position);
        holder.time.setText(ExplorerFormat.time(context, item.time));
        holder.height.setText(context.getString(R.string.explorer_height,
                measurement(item.waveHeight, R.string.explorer_metres)));
        holder.period.setText(context.getString(R.string.explorer_period,
                measurement(item.wavePeriod, R.string.explorer_seconds)));
        holder.direction.setText(context.getString(R.string.explorer_direction, direction(item.waveDirection)));
        holder.temperature.setText(context.getString(R.string.explorer_water,
                measurement(item.waterTemperature, R.string.explorer_celsius)));
        if (item.waveHeight == null) {
            holder.wave.setImageResource(R.drawable.ic_wave_unknown);
            holder.surf.setText(R.string.explorer_surf_unknown);
        } else if (item.exceedsWaveThreshold(settings)) {
            holder.wave.setImageResource(R.drawable.ic_surf);
            holder.surf.setText(R.string.explorer_surf_good);
        } else {
            holder.wave.setImageResource(R.drawable.ic_wave);
            holder.surf.setText(R.string.explorer_surf_below);
        }
        holder.favorite.setVisibility(favorites.contains(item.key()) ? View.VISIBLE : View.GONE);
        return convertView;
    }

    private String measurement(@Nullable Double value, int formatRes) {
        return ExplorerFormat.measurement(context, value, formatRes);
    }

    private String direction(@Nullable Double degrees) {
        return ExplorerFormat.direction(context, degrees);
    }

    /** Referências encontradas apenas na criação da linha, em vez de em cada getView(). */
    private static final class ViewHolder {
        final TextView time, height, period, direction, temperature, surf;
        final ImageView wave, favorite;

        ViewHolder(View row) {
            time = row.findViewById(R.id.tvMarineTime);
            height = row.findViewById(R.id.tvWaveHeight);
            period = row.findViewById(R.id.tvWavePeriod);
            direction = row.findViewById(R.id.tvWaveDirection);
            temperature = row.findViewById(R.id.tvWaterTemperature);
            surf = row.findViewById(R.id.tvSurfLevel);
            wave = row.findViewById(R.id.ivWave);
            favorite = row.findViewById(R.id.ivMarineFavorite);
        }
    }
}

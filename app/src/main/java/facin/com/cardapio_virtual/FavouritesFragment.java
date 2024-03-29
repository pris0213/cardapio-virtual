package facin.com.cardapio_virtual;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import facin.com.cardapio_virtual.auxiliares.SimpleDividerItemDecoration;
import facin.com.cardapio_virtual.auxiliares.Utilitarios;
import facin.com.cardapio_virtual.data.DatabaseContract;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class FavouritesFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private Cursor restaurantsCursor;
    private List<Restaurant> favoritos;
    private RecyclerView recyclerView;
    private View view;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FavouritesFragment() {
        // favoritos = new ArrayList<>();
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static FavouritesFragment newInstance(int columnCount) {
        FavouritesFragment fragment = new FavouritesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
        favoritos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_favourite_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            // new FetchFavouriteTask().execute((Void) null);
            atualizaFavoritos();
            //recyclerView.setAdapter(new FavouriteRecyclerViewAdapter(DummyContent.ITEMS, mListener));
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        atualizaFavoritos();
//    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Restaurant item);
    }

    public void atualizaFavoritos() {
        try {
            new FetchFavouriteTask().execute((Void) null).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void atualizaRecyclerView() {
        recyclerView.setAdapter(new FavouriteRecyclerViewAdapter(favoritos, mListener));
    }

    public class FetchFavouriteTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // TODO: Esta query é um join. Criar join.
                restaurantsCursor = getActivity().getContentResolver().query(
                        DatabaseContract.RestaurantesEntry.CONTENT_URI,
                        null,
                        DatabaseContract.RestaurantesEntry.COLUMN_FAVORITO + " = ?",
                        new String[]{"1"},
                        null
                );
                if (restaurantsCursor != null) {
                    return true;
                }
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                return false;
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            if (result) {
                favoritos = populaLista(restaurantsCursor);
                Utilitarios.ordenaRestaurantes(favoritos, MainActivity.mLastLocation);
                recyclerView.setAdapter(new FavouriteRecyclerViewAdapter(favoritos, mListener));
            }
        }

        public List<Restaurant> populaLista(Cursor cursor) {
            favoritos = new ArrayList<>();
            DatabaseUtils.dumpCursor(restaurantsCursor);
            /* Cria restaurantes */
            while(restaurantsCursor.moveToNext()) {
                Restaurant restaurant = new Restaurant(
                        Integer.parseInt(restaurantsCursor.getString(0)),
                        restaurantsCursor.getString(1),
                        restaurantsCursor.getString(2),
                        restaurantsCursor.getString(3),
                        restaurantsCursor.getString(4),
                        Double.parseDouble(restaurantsCursor.getString(5)),
                        Double.parseDouble(restaurantsCursor.getString(6)),
                        restaurantsCursor.getString(7),
                        !cursor.getString(8).equals("0")
                );
                favoritos.add(restaurant);
            }
            return favoritos;
        }
    }

    public List<Restaurant> getFavoritos() {
        return favoritos;
    }

    public void setFavoritos(List<Restaurant> favoritos) {
        this.favoritos = favoritos;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }
}

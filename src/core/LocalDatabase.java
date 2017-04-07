package core;

import java.util.ArrayList;
import java.util.List;

/**
 * Local database which stores {@link DisasterData} along with
 * the size of the database. Offers function to delete old and far away data
 * while dynamically setting the deletion threshold depending on how full the
 * database is.
 *
 * Created by melanie on 07.04.17.
 */
public class LocalDatabase {
    /* size of the database in kB */
    private int size;
    private List<DisasterData> dataList =new ArrayList<>();
    /**
     * when the utility of a data item is below this threshold,
     * it will be deleted.
     */
    private double deletionThreshold;

    /**
     * Adds a dataItem to the database and
     * checks whether any old data needs to be deleted
     * @param newDataItem
     */
    public void add (DisasterData newDataItem, Coord location, double time){
        dataList.add(newDataItem);
        size+=newDataItem.getSize();
        deleteIrrelevantData(location, time);
    }

    /**
     * Checks all data items whether their utility
     * is below the threshold
     */
    private void deleteIrrelevantData(Coord location, double time) {
        computeDeletionThreshold();
        for (DisasterData dataItem : dataList){
            if (computeUtility(dataItem, location, time)<deletionThreshold){
                size-=dataItem.getSize();
                dataList.remove(dataItem);
            }
        }
    }

    private void computeDeletionThreshold() {
        //TODO
    }

    List<DisasterData> getAllDataWithMinimumUtility(double minUtility, Coord location, double time){
        List <DisasterData> dataWithMinUtility = new ArrayList<>();
        for (DisasterData dataItem : dataList) {
            if (computeUtility(dataItem, location, time)>=minUtility){
                dataWithMinUtility.add(dataItem);
            }
        }

    }

    private double computeUtility(DisasterData dataItem, Coord location, double time) {
        //Distance between data and current location
        //The farther away an item is, the lower its utility
        double distance = dataItem.getLocation().distance(location);
        //How long it has been since the data item has been created
        //The older an item is, the lower its utility
        double age = time - dataItem.getCreation();
        //Factor how much distance influences the utility in comparison to result
        double alpha=0.5;
        //Factor how fast aging occurs, the higher gamma, the faster the aging
        double gamma= 2;

        switch (dataItem.getType()){
            case MAP:
                //For maps we just regard the distance, as map data does not become outdated
                //within the disaster time frame
                alpha=1;
            case SKILL:
                //For skills, the aging is slower, as skills will likely not fade
                //The importance on distance is also lower
                alpha=0.3;
                gamma=1.1;
            case RESOURCE:
                //For resources the importance of distance is lower
                alpha=0.3;
        }
        //util(dataItem) = α * 2^(-distance/2)  + (1-α) *  γ^(-age)
        return alpha*Math.pow(2,-(distance/2))+(1-alpha)*Math.pow(gamma,(-age));
    }

    /**
     *
     * @return size of the database in kB
     */
    public int getSize() {
        return size;
    }
}

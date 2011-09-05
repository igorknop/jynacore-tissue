/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgmc.jynacore;

import br.ufjf.mmc.jynacore.JynaSimulableModel;
import br.ufjf.mmc.jynacore.JynaSimulation;
import br.ufjf.mmc.jynacore.JynaSimulationMethod;
import br.ufjf.mmc.jynacore.JynaSimulationProfile;
import br.ufjf.mmc.jynacore.JynaValued;
import br.ufjf.mmc.jynacore.impl.DefaultSimulationData;
import br.ufjf.mmc.jynacore.impl.DefaultSimulationProfile;
import br.ufjf.mmc.jynacore.metamodel.MetaModel;
import br.ufjf.mmc.jynacore.metamodel.MetaModelStorer;
import br.ufjf.mmc.jynacore.metamodel.exceptions.instance.MetaModelInstanceInvalidLinkException;
import br.ufjf.mmc.jynacore.metamodel.impl.JDOMMetaModelStorer;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceItem;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceStock;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.impl.DefaultMetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceEulerMethod;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceSimulation;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import mpi.*;

/**
 *
 * @author igor
 */
public class JynacoreTissueMPJ {


   static final int ROWS = 5;                 /* number of rows in tissue */

   static final int COLS = 1;                 /* number of columns tissue */

   static final int MASTER = 0;                /* taskid of first task */

   static final int FROM_MASTER = 1;           /* setting a message type */

   static final int FROM_WORKER = 2;           /* setting a message type */

   private static final double TIME_INITIAL = 0.0;
   private static final double TIME_FINAL = 5.0;
   public static final int TIME_STEPS = 50;
   private static final int TIME_SKIP = 10;
   private static final Logger logger = Logger.getLogger("JynacoreTissueMPJ");

   /**
    * @param args the command line arguments
    */
   public static void main(String[] args) throws Exception {
      int numtasks, /* number of tasks in partition */
              taskid, /* a task identifier */
              numworkers, /* number of worker tasks */
              rows = 0, /* rows to sent to each worker */
              offset = 0; /* used to determine rows sent to each worker */

      MPI.Init(args);
      taskid = MPI.COMM_WORLD.Rank();
      numtasks = MPI.COMM_WORLD.Size();
      numworkers = numtasks - 1;

      MetaModelStorer storer = new JDOMMetaModelStorer();
      MetaModel metamodel = storer.loadFromFile(new File("planar.jymm"));
      MetaModelInstance metaModelInstance = new DefaultMetaModelInstance();
      metaModelInstance.setMetaModel(metamodel);



      DefaultSimulationData data = new DefaultSimulationData();
      data.clearAll();
      createCells(metaModelInstance, data);
      connectCells(metaModelInstance);

      if (taskid == MASTER) {
         JynaSimulation simulation = new DefaultMetaModelInstanceSimulation();
         JynaSimulationProfile profile = new DefaultSimulationProfile();
         JynaSimulationMethod method = new DefaultMetaModelInstanceEulerMethod();

         profile.setInitialTime(TIME_INITIAL);
         profile.setFinalTime(TIME_FINAL);
         profile.setTimeSteps(TIME_STEPS);


         simulation.setProfile(profile);
         simulation.setMethod(method);
         simulation.setModel(metaModelInstance);
         logger.log(Level.INFO, "M:  Reseting simulation initial values (step -1)\n");
         simulation.reset();
         data.register(0.0);
         logger.log(Level.INFO, "M:  data before processing in workers:\n{0}", data);
         
         logger.log(Level.INFO, "M:  Entering in simulation loop:\n");
         for (int step = 0; step < TIME_STEPS; step++) {
            logger.log(Level.INFO, "M: \titeration: {0}\n", step);
            logger.log(Level.INFO, "M:   cell[0,0] = {0}", ((ClassInstanceStock) metaModelInstance.getClassInstances().get("cell[0,0]").get("Value")).getValue());

            masterSendCellsToWorkers(numworkers, metaModelInstance);
            masterReceiveCellsFromWorkers(numworkers, metaModelInstance, data);
            logger.log(Level.INFO, "M:   cell[0,0] = {0}", ((ClassInstanceStock) metaModelInstance.getClassInstances().get("cell[0,0]").get("Value")).getValue());
            if (step % TIME_SKIP == 0) {
               data.register(TIME_INITIAL + step * (TIME_FINAL - TIME_INITIAL) / TIME_STEPS);
            }
         }
         logger.log(Level.INFO, "M:  data after processing in workers:\n{0}", data);
      }

      //**************************** worker task ************************************/
      if (taskid > MASTER) {
         JynaSimulation simulation = new DefaultMetaModelInstanceSimulation();
         JynaSimulationProfile profile = new DefaultSimulationProfile();
         JynaSimulationMethod method = new DefaultMetaModelInstanceEulerMethodMPJ2();

         profile.setInitialTime(TIME_INITIAL);
         profile.setFinalTime(TIME_FINAL);
         profile.setTimeSteps(TIME_STEPS);


         simulation.setProfile(profile);
         simulation.setMethod(method);
         //simulation.setModel(metaModelInstance);
         Map<String, Integer> params;
         for (int step = 0; step < TIME_STEPS; step++) {
            params = workerReceiveCellsFromMaster(taskid, offset, rows, (MetaModelInstance) metaModelInstance);
            logger.log(Level.INFO, "W{0}:   Starting computing", taskid);
            ((DefaultMetaModelInstanceEulerMethodMPJ2) method).setOffset(params.get("offset"));
            ((DefaultMetaModelInstanceEulerMethodMPJ2) method).setRows(params.get("rows"));
            ((DefaultMetaModelInstanceEulerMethodMPJ2) method).setCols(COLS);
            simulation.setModel(metaModelInstance);
            logger.log(Level.INFO, "W{0}:   cell[0,0] = {1}", new Object[]{taskid, ((ClassInstanceStock) metaModelInstance.getClassInstances().get("cell[0,0]").get("Value")).getValue()});
            simulation.step();
            logger.log(Level.INFO, "W{0}:   cell[0,0] = {1}", new Object[]{taskid, ((ClassInstanceStock) metaModelInstance.getClassInstances().get("cell[0,0]").get("Value")).getValue()});
            logger.log(Level.INFO, "W{0}:   Done computing", taskid);
            workerSendCellsToMaster(params.get("offset"), params.get("rows"), taskid, metaModelInstance);
         }
      } // end of worker

      MPI.Finalize();
   }

   private static void masterReceiveCellsFromWorkers(int numworkers, MetaModelInstance metamodelInstance, DefaultSimulationData data) throws MPIException {
      int mtype;
      int rows;
      int count;
      int offset;
      /* wait for results from all worker tasks */
      mtype = FROM_WORKER;
      int[] buffrecv = new int[1];
      for (int source = 1; source <= numworkers; source++) {
         MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
         offset = buffrecv[0];
         logger.log(Level.INFO, "M: received offset=\"{0}\" from W{1}", new Object[]{buffrecv[0], source});
         MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
         rows = buffrecv[0];
         logger.log(Level.INFO, "M: received rows=\"{0}\" from W{1}", new Object[]{buffrecv[0], source});
         count = rows * COLS;
         MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
         int ocount = buffrecv[0];
         logger.log(Level.INFO, "M: received: object count={1} from W{0}", new Object[]{source, ocount});

         Object[] buffRecvObject = new Object[ocount];
         MPI.COMM_WORLD.Recv(buffRecvObject, 0, ocount, MPI.OBJECT, source, mtype);
         logger.log(Level.INFO, "M: received: {1} objects  from W{0}", new Object[]{source, buffRecvObject.length});
         for (int i = 0; i < ocount-1; i += 2) {
            String ciString = (String) buffRecvObject[i];
            logger.log(Level.INFO, "M: parsing: {1} from W{0}", new Object[]{source, ciString});
            String[] ciNameTokens = ciString.split("\\.");
            String ciName = ciNameTokens[0];
            String ciField = ciNameTokens[1];
            Double ciValue = (Double) buffRecvObject[i + 1];
            ClassInstance ci = metamodelInstance.getClassInstances().get(ciName);
            ClassInstanceItem cii = ci.get(ciField);
            logger.log(Level.INFO, "M:  updating {1}.{2}={3} to {4} from worker  {0}", new Object[]{source, ciName, ciField, ((ClassInstanceStock)cii).getValue(),ciValue});
            ((ClassInstanceStock) cii).setValue(ciValue);
         }
      }
   }

   private static void masterSendCellsToWorkers(int numworkers, MetaModelInstance metamodelInstance) throws MPIException {
      int extra;
      int averow;
      int rows;
      int mtype;
      int cellCount;
      int dest;
      int offset;
      /* send cells data to the worker tasks */
      averow = ROWS / numworkers;
      extra = ROWS % numworkers;
      offset = 0;
      mtype = FROM_MASTER;
      int[] buffSendInt = new int[1];
      for (dest = 1; dest <= numworkers; dest++) {
         rows = (dest <= extra) ? averow + 1 : averow;
         logger.log(Level.INFO, "M: Preparing to send {0} rows to W{1}", new Object[]{rows, dest});
         buffSendInt[0] = offset;
         logger.log(Level.INFO, "M: sending offset=\"{0}\" to W{1}", new Object[]{buffSendInt[0], dest});
         MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, dest, mtype);
         buffSendInt[0] = rows;
         logger.log(Level.INFO, "M: sending rows=\"{0}\" to W{1}", new Object[]{buffSendInt[0], dest});
         MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, dest, mtype);
         cellCount = ROWS * COLS;

         Map<String, Object> omap = new HashMap<String, Object>();

         for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
            String ciName = "cell[" + (cellIndex / COLS) + "," + cellIndex % COLS + "]";
            ClassInstance ci = metamodelInstance.getClassInstances().get(ciName);
            for (Entry<String, ClassInstanceItem> entrycii : ci.entrySet()) {
               if (entrycii.getValue() instanceof ClassInstanceStock) {
                  omap.put(ci.getName() + "." + entrycii.getKey(), ((ClassInstanceStock) entrycii.getValue()).getValue());
               }
            }
         }
         logger.log(Level.INFO, "M: Found {0} levels to send to W{1}", new Object[]{omap.size(), dest});
         int countToSend = omap.size() * 2;
         Object[] buffSendObject = new Object[countToSend];
         int oindex = 0;
         for (Entry<String, Object> e : omap.entrySet()) {
            buffSendObject[oindex] = e.getKey();
            buffSendObject[oindex+1] = e.getValue();
            oindex +=2;
         }

         buffSendInt[0] = countToSend;
         logger.log(Level.INFO, "M: sending object count=\"{0}\" to W{1}", new Object[]{buffSendInt[0], dest});
         MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, dest, mtype);

         logger.log(Level.INFO, "M: Sending {0} objects to W{1}", new Object[]{buffSendObject.length, dest});
         MPI.COMM_WORLD.Send(buffSendObject, 0, countToSend, MPI.OBJECT, dest, mtype);
         offset = offset + rows;
      }
   }

   private static void printMeshValues(DefaultSimulationData data) {
      System.out.println(data.getWatchedNames());
      System.out.println(data);
      for (int i = 0; i < ROWS; i++) {
         for (int j = 0; j < COLS; j++) {
         }
      }
   }

   private static void runSimulation(JynaSimulation simulation, int skip) throws Exception {
      //simulation.run();
      int steps = simulation.getProfile().getTimeSteps();

      logger.log(Level.INFO, "Simulating with {0} iterations.", steps);
      for (int i = 0;
              i < steps;
              i++) {
         simulation.step();
         if (i % skip == 0) {
            simulation.register();
         }
      }
      logger.log(Level.INFO, "Simulating done!");
   }

   private static void connectCells(JynaSimulableModel instance) throws MetaModelInstanceInvalidLinkException {
      MetaModelInstance mmi = (MetaModelInstance) instance;
      logger.log(Level.INFO, "Creating {0} class instance relations.", 4 * ROWS * COLS);
      for (int i = 0; i < ROWS; i++) {
         for (int j = 0; j < COLS; j++) {
            ClassInstance eci = mmi.getClassInstances().get("cell[" + i + "," + j + "]");
            eci.setLink("east", "cell[" + i + "," + ((j == COLS - 1) ? j : j + 1) + "]");
            eci.setLink("west", "cell[" + i + "," + ((j == 0) ? j : j - 1) + "]");
            eci.setLink("north", "cell[" + ((i == 0) ? i : i - 1) + "," + j + "]");
            eci.setLink("south", "cell[" + ((i == ROWS - 1) ? i : i + 1) + "," + j + "]");
         }
      }
   }

   private static MetaModelInstance createCells(JynaSimulableModel instance, DefaultSimulationData data) throws Exception {
      instance.setName("Tissue");
      MetaModelInstance mmi = (MetaModelInstance) instance;
      logger.log(Level.INFO, "Creating {0} class instances.", (ROWS * COLS));
      for (int i = 0; i < ROWS; i++) {
         for (int j = 0; j < COLS; j++) {
            ClassInstance ci = mmi.addNewClassInstance("cell[" + i + "," + j + "]", "Cell");
            ci.setProperty("InitialValue", (i == 0 && j == 0) ? 100.0 : 0.0);
            data.add("cell[" + i + "," + j + "]", (JynaValued) ci.get("Value"));
         }
      }
      return mmi;
   }

   private static Map<String, Integer> workerReceiveCellsFromMaster(int taskid, int offset, int rows, MetaModelInstance modelInstance) {
      Map<String, Integer> params = new HashMap<String, Integer>();
      int[] buffrecv = new int[1];
      int mtype = FROM_MASTER;
      int source = MASTER;
      logger.log(Level.INFO, "W{0}:  Master ={1}, mtype={2}", new Object[]{taskid, source, mtype});

      MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
      offset = buffrecv[0];
      logger.log(Level.INFO, "W{0}:  received: offset={1}", new Object[]{taskid, offset});
      params.put("offset", offset);
      
      MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
      rows = buffrecv[0];
      logger.log(Level.INFO, "W{0}:  received: rows={1}", new Object[]{taskid, rows});
      params.put("rows", rows);
      
      MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
      int ocount = buffrecv[0];
      logger.log(Level.INFO, "W{0}:  received: object count={1}", new Object[]{taskid, ocount});

      Object[] buffRecvObject = new Object[ocount];
      MPI.COMM_WORLD.Recv(buffRecvObject, 0, ocount, MPI.OBJECT, source, mtype);
      logger.log(Level.INFO, "W{0}:  received: {1} objects", new Object[]{taskid, buffRecvObject.length});
      for (int i = 0; i < ocount-1; i += 2) {
         String ciString = (String) buffRecvObject[i];
         Double ciValue = (Double) buffRecvObject[i + 1];
         logger.log(Level.INFO, "W{0}: parsing: {1} = {2}", new Object[]{taskid, ciString, ciValue});
         String[] ciNameTokens = ciString.split("\\.");
         String ciName = ciNameTokens[0];
         String ciField = ciNameTokens[1];

         ClassInstance ci = modelInstance.getClassInstances().get(ciName);
         ClassInstanceItem cii = ci.get(ciField);
         logger.log(Level.INFO, "W{0}:  setting {1}.{2}={3} to {4}", new Object[]{taskid, ciName, ciField, ((ClassInstanceStock)cii).getValue(), ciValue});
         ((ClassInstanceStock) cii).setValue(ciValue);
      }

      return params;
   }

   private static void workerSendCellsToMaster(int offset, int rows, int taskid, JynaSimulableModel instance) {
      int mtype = FROM_WORKER;
      int[] buffSendInt = new int[1];
      buffSendInt[0] = offset;
      logger.log(Level.INFO, "W{0}: sending offset={1} to M{2}", new Object[]{taskid, buffSendInt[0], MASTER});
      MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, MASTER, mtype);
      buffSendInt[0] = rows;
      logger.log(Level.INFO, "W{0}: sending rows={1} to M{2}", new Object[]{taskid, buffSendInt[0], MASTER});
      MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, MASTER, mtype);
      int cellCount = rows * COLS;

      Map<String, Object> omap = new HashMap<String, Object>();
      MetaModelInstance mmi = (MetaModelInstance) instance;
      for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
         String ciName = "cell[" + (offset + cellIndex / COLS) + "," + cellIndex % COLS + "]";
         ClassInstance ci = mmi.getClassInstances().get(ciName);
         logger.log(Level.INFO, "W{0}: looking for {1}: {2}", new Object[]{taskid, ciName, ci!=null});
         for (Entry<String, ClassInstanceItem> entry : ci.entrySet()) {
            if (entry.getValue() instanceof ClassInstanceStock) {
               omap.put(ci.getName() + "." + entry.getKey(), ((ClassInstanceStock) entry.getValue()).getValue());
            }
         }
      }
      logger.log(Level.INFO, "W{0}: found {1} levels to send to M", new Object[]{taskid, omap.size()});
      int countToSend = omap.size() * 2;
      Object[] buffSendObject = new Object[countToSend];
      int oindex = 0;
      for (Entry<String, Object> e : omap.entrySet()) {
         buffSendObject[oindex] = e.getKey();
         buffSendObject[oindex+1] = e.getValue();
         oindex += 2;
      }
      buffSendInt[0] = countToSend;
      logger.log(Level.INFO, "W{0}: sending object count=\"{1}\" to M", new Object[]{taskid, buffSendInt[0]});
      MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, MASTER, mtype);

      logger.log(Level.INFO, "W{0}: Sending {1} objects to M: {2}", new Object[]{taskid, countToSend, buffSendObject});
      MPI.COMM_WORLD.Send(buffSendObject, 0, countToSend, MPI.OBJECT, MASTER, mtype);
   }
}

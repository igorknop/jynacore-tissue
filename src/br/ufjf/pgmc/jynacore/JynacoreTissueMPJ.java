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

   public static final int TIME_STEPS = 50;
   static final int ROWS = 1;                 /* number of rows in tissue */

   static final int COLS = 1;                 /* number of columns tissue */

   static final int MASTER = 0;                /* taskid of first task */

   static final int FROM_MASTER = 1;           /* setting a message type */

   static final int FROM_WORKER = 2;           /* setting a message type */

   private static final double TIME_FINAL = 5.0;
   private static final double TIME_INITIAL = 0.0;
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
      JynaSimulableModel metaModelInstance = new DefaultMetaModelInstance();
      ((MetaModelInstance) metaModelInstance).setMetaModel(metamodel);



         DefaultSimulationData data = new DefaultSimulationData();
         data.clearAll();
         createCells(metaModelInstance, data);
         connectCells(metaModelInstance);
         
      if (taskid == MASTER) {
         data.register(0.0);
         logger.log(Level.INFO, "MASTER:\n Entering in simulation loop:\n");
         for (int step = 0; step < TIME_STEPS; step++) {
            logger.log(Level.INFO, "MASTER:\n\titeration: {0}\n", step);
            masterSendCellsToWorkers(numworkers, metaModelInstance);
            masterReceiveCellsFromWorkers(numworkers, metaModelInstance, data);
            if (step % TIME_SKIP == 0) {
               data.register(TIME_INITIAL + step * (TIME_FINAL - TIME_INITIAL) / TIME_STEPS);
            }
         }
         logger.log(Level.INFO, "MASTER:\n data after processing in workers:\n{0}", data);
      }

      //**************************** worker task ************************************/
      if (taskid > MASTER) {
         JynaSimulation simulation = new DefaultMetaModelInstanceSimulation();
         JynaSimulationProfile profile = new DefaultSimulationProfile();
         JynaSimulationMethod method = new DefaultMetamodelInstanceEulerMethodMPJ();

         profile.setInitialTime(TIME_INITIAL);
         profile.setFinalTime(TIME_FINAL);
         profile.setTimeSteps(TIME_STEPS);


         simulation.setProfile(profile);
         simulation.setMethod(method);
         // simulation.reset();
         for (int step = 0; step < TIME_STEPS; step++) {
            workerReceiveCellsFromMaster(taskid, offset, rows, (MetaModelInstance) metaModelInstance);
            //TODO - Simulation here
            logger.log(Level.INFO, "WORKER {0}:\n Starting computing", taskid);
//            ((DefaultMetamodelInstanceEulerMethodMPJ) method).setOffset(offset);
//            ((DefaultMetamodelInstanceEulerMethodMPJ) method).setRows(rows);
//            ((DefaultMetamodelInstanceEulerMethodMPJ) method).setCols(COLS);
//            simulation.setModel(instance);
//            simulation.step();
            logger.log(Level.INFO, "WORKER {0}:\n Done computing", taskid);
            workerSendCellsToMaster(offset, rows, taskid, metaModelInstance);
         }
      } // end of worker

      MPI.Finalize();
   }

   private static void masterReceiveCellsFromWorkers(int numworkers, JynaSimulableModel instance, DefaultSimulationData data) throws MPIException {
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
         logger.log(Level.INFO, "MASTER:\n\n\treceived offset=\"{0}\" from task {1}", new Object[]{buffrecv[0], source});
         MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
         rows = buffrecv[0];
         logger.log(Level.INFO, "MASTER:\n\n\treceived rows=\"{0}\" from task {1}", new Object[]{buffrecv[0], source});
         count = rows * COLS;
         Object[] buffRecvObject = new Object[count];
         MPI.COMM_WORLD.Recv(buffRecvObject, 0, count, MPI.OBJECT, source, mtype);
         logger.log(Level.INFO, "MASTER:\n\n\tReceived Class Instances from task {0} : {1}\n\t", new Object[]{source, buffRecvObject});
         for (int i = 0; i < count; i++) {
            MetaModelInstance mmi = (MetaModelInstance) instance;
            mmi.getClassInstances().put("cell[" + offset + i / COLS + "," + i % COLS + "]", (ClassInstance) buffRecvObject[i]);
         }
      }
   }

   private static void masterSendCellsToWorkers(int numworkers, JynaSimulableModel instance) throws MPIException {
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
         logger.log(Level.INFO, "MASTER:\n\tsending {0} rows to task {1}", new Object[]{rows, dest});
         buffSendInt[0] = offset;
         logger.log(Level.INFO, "MASTER:\n\tsending offset=\"{0}\" to task {1}", new Object[]{buffSendInt[0], dest});
         MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, dest, mtype);
         buffSendInt[0] = rows;
         logger.log(Level.INFO, "MASTER:\n\tsending rows=\"{0}\" to task {1}", new Object[]{buffSendInt[0], dest});
         MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, dest, mtype);
         cellCount = rows * COLS;

         Map<String, Object> omap = new HashMap<String, Object>();
         MetaModelInstance mmi = (MetaModelInstance) instance;
         for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
            String ciName = "cell[" + (offset + cellIndex / COLS) + "," + cellIndex % COLS + "]";
            ClassInstance ci = mmi.getClassInstances().get(ciName);
            for (Entry<String, ClassInstanceItem> entrycii : ci.entrySet()) {
               if (entrycii.getValue() instanceof ClassInstanceStock) {
                  omap.put(ci.getName() + "." + entrycii.getKey(), ((ClassInstanceStock) entrycii.getValue()).getValue());
                  logger.log(Level.INFO, "MASTER:\n\t\tFound {0} total:\"{1}\" to task {2}", new Object[]{entrycii.getKey(), omap.size(), dest});
               }
            }
         }
         int countToSend = omap.size() * 2;
         Object[] buffSendObject = new Object[countToSend];
         int oindex = 0;
         for (Entry<String, Object> e : omap.entrySet()) {
            buffSendObject[oindex] = e.getKey();
            buffSendObject[++oindex] = e.getValue();
         }

         buffSendInt[0] = countToSend;
         logger.log(Level.INFO, "MASTER:\n\tsending level count=\"{0}\" to task {1}", new Object[]{buffSendInt[0], dest});
         MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, dest, mtype);

         logger.log(Level.INFO, "MASTER:\n\n\tSending MetaModelClassInstances to worker {1}: {0} \n\t", new Object[]{buffSendObject, dest});
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

   private static void workerReceiveCellsFromMaster(int taskid, int offset, int rows, MetaModelInstance modelInstance) {
      int[] buffrecv = new int[1];
      int mtype = FROM_MASTER;
      int source = MASTER;
      logger.log(Level.INFO, "WORKER {0}:\nMaster ={1}, mtype={2}", new Object[]{taskid, source, mtype});

      MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
      offset = buffrecv[0];
      logger.log(Level.INFO, "WORKER {0}\n received: offset={1}", new Object[]{taskid, offset});

      MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
      rows = buffrecv[0];
      logger.log(Level.INFO, "WORKER {0}\n received: rows={1}", new Object[]{taskid, rows});

      MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
      int ocount = buffrecv[0];
      logger.log(Level.INFO, "WORKER {0}\n received: level count={1}", new Object[]{taskid, ocount});


      int cellCount = rows * COLS;
      Object[] buffRecvObject = new Object[ocount];
      MPI.COMM_WORLD.Recv(buffRecvObject, 0, ocount, MPI.OBJECT, source, mtype);
      logger.log(Level.INFO, "WORKER {0}\n received: {1} objects", new Object[]{taskid, buffRecvObject.length}); 
      for (int i = 0; i < ocount; i += 2) {
         String ciString = (String) buffRecvObject[i];
         logger.log(Level.INFO, "WORKER {0}\n\t\tString to parse: {1}", new Object[]{taskid,ciString});
         String[] ciNameTokens = ciString.split("\\.");
         String ciName = ciNameTokens[0];
         String ciField = ciNameTokens[1];
         Double ciValue =  (Double) buffRecvObject[i + 1];
         ClassInstance ci = modelInstance.getClassInstances().get(ciName);
         ClassInstanceItem cii = ci.get(ciField);
         ((ClassInstanceStock) cii).setValue(ciValue);
         logger.log(Level.INFO, "WORKER {0}\n setting {1}.{2}={4} to {3}", new Object[]{taskid, ciName, ciField, cii.getName(), buffRecvObject[i + 1]});
      }

   }

   private static void workerSendCellsToMaster(int offset, int rows, int taskid, JynaSimulableModel instance) {
      int mtype = FROM_WORKER;
      int[] buffSendInt = new int[1];
      buffSendInt[0] = offset;
      logger.log(Level.INFO, "WORKER {0}:\n Sending offset={1} to {2}", new Object[]{taskid, buffSendInt[0], MASTER});
      MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, MASTER, mtype);
      buffSendInt[0] = rows;
      logger.log(Level.INFO, "WORKER {0}:\n Sending offset={1} to {2}", new Object[]{taskid, buffSendInt[0], MASTER});
      MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, MASTER, mtype);
      int count = rows * COLS;
      Object[] buffSendObject = new Object[count];
      int i;
      for (i = 0; i < count; i++) {
         buffSendObject[i] = (Object) ((MetaModelInstance) instance).getClassInstances().get("cell[" + offset + i / COLS + "," + i % COLS + "]");
      }
      logger.log(Level.INFO, "WORKER {0}:\n sending class instances to {1}", new Object[]{taskid, MASTER});
      MPI.COMM_WORLD.Send(buffSendObject, 0, count, MPI.OBJECT, MASTER, mtype);
      //logger.log(Level.INFO, "WORKER {0}:\n data after processing:\n{1}", new Object[]{taskid, null});
   }
}

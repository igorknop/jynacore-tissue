/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgmc.jynacore;

import br.ufjf.mmc.jynacore.expression.impl.DefaultNumberConstantExpression;
import br.ufjf.mmc.jynacore.metamodel.exceptions.instance.MetaModelInstanceException;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceAuxiliary;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceItem;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceMultiRelation;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceProperty;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceRate;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceSingleRelation;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceStock;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceEulerMethod;
import br.ufjf.mmc.jynacore.systemdynamics.Variable;
import br.ufjf.mmc.jynacore.systemdynamics.impl.DefaultVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author igor
 */
public class DefaultMetamodelInstanceEulerMethodMPJ extends DefaultMetaModelInstanceEulerMethod {

   private int offset;
   private int rows;
   private int cols;
   private Double initialTime;
   private Map<String, ClassInstanceRate> rates;
   private Map<String, ClassInstanceStock> levels;
   private HashMap<String, ClassInstanceAuxiliary> auxiliaries;
   private HashMap<String, ClassInstanceProperty> properties;
   private double currentTime;
   private int currentStep;
   private Double stepSize;
   private MetaModelInstance modelInstance;
   private Map<String, ClassInstanceSingleRelation> singleRelations;
   private Map<String, ClassInstanceMultiRelation> multiRelations;
   private Variable _TIME_;
   private Variable _TIME_STEP_;
   private static final Logger logger = Logger.getLogger("DefaultMetamodelInstanceEulerMethodMPJ");

   public DefaultMetamodelInstanceEulerMethodMPJ() {
      rates = new HashMap<String, ClassInstanceRate>();
      levels = new HashMap<String, ClassInstanceStock>();
      auxiliaries = new HashMap<String, ClassInstanceAuxiliary>();
      properties = new HashMap<String, ClassInstanceProperty>();
      singleRelations = new HashMap<String, ClassInstanceSingleRelation>();
      multiRelations = new HashMap<String, ClassInstanceMultiRelation>();
      currentTime = 0.0;
      currentStep = 0;
      initialTime = 0.0;
      _TIME_ = new DefaultVariable();
      _TIME_.setName("_TIME_");
      _TIME_.setExpression(new DefaultNumberConstantExpression(currentTime));
      _TIME_STEP_ = new DefaultVariable();
      _TIME_STEP_.setName("_TIME_STEP_");
      _TIME_STEP_.setExpression(new DefaultNumberConstantExpression(stepSize));
   }

   public int getCols() {
      return cols;
   }

   public void setCols(int cols) {
      this.cols = cols;
   }

   public int getOffset() {
      return offset;
   }

   public void setOffset(int offset) {
      this.offset = offset;
   }

   public int getRows() {
      return rows;
   }

   public void setRows(int rows) {
      this.rows = rows;
   }

   @Override
   public void setMetaModelInstance(MetaModelInstance modelInstance) throws Exception {
      this.modelInstance = modelInstance;
      rates.clear();
      levels.clear();
      auxiliaries.clear();
      properties.clear();
      singleRelations.clear();
      multiRelations.clear();
      logger.log(Level.INFO, "Going to add only classinstances in offset={0} rows={1} cols={2}!", new Object[]{getOffset(), getRows(), getCols()});
      for (int i = offset; i < rows; i++) {
         for (int j = 0; j < cols; j++) {
            String classInstanceName = "cell[" + i + "," + j + "]";
            ClassInstance classInstance = modelInstance.getClassInstances().get(classInstanceName);
            logger.log(Level.INFO, "Looking for class instance {0}: {1}!", new Object[]{classInstanceName, classInstance == null ? "Missing" : "Found"});
            for (Entry<String, ClassInstanceItem> ciItem : classInstance.entrySet()) {
               if (ciItem.getValue() instanceof ClassInstanceStock) {
                  ClassInstanceStock ciLevel = (ClassInstanceStock) ciItem.getValue();
                  String ciLevelName = ciItem.getKey();
                  levels.put(getKey(classInstanceName, ciLevelName), ciLevel);
               } else if (ciItem.getValue() instanceof ClassInstanceRate) {
                  ClassInstanceRate ciRate = (ClassInstanceRate) ciItem.getValue();
                  String ciRateName = ciItem.getKey();
                  rates.put(getKey(classInstanceName, ciRateName), ciRate);
               } else if (ciItem.getValue() instanceof ClassInstanceAuxiliary) {
                  ClassInstanceAuxiliary ciProc = (ClassInstanceAuxiliary) ciItem.getValue();
                  String ciProcName = ciItem.getKey();
                  auxiliaries.put(getKey(classInstanceName, ciProcName),
                          ciProc);
               } else if (ciItem.getValue() instanceof ClassInstanceProperty) {
                  ClassInstanceProperty ciProperty = (ClassInstanceProperty) ciItem.getValue();
                  String ciPropertyName = ciItem.getKey();
                  properties.put(getKey(classInstanceName, ciPropertyName),
                          ciProperty);
               } else if (ciItem.getValue() instanceof ClassInstanceSingleRelation) {
                  ClassInstanceSingleRelation ciSingleRelation = (ClassInstanceSingleRelation) ciItem.getValue();
                  String ciSingleRelationName = ciItem.getKey();
                  singleRelations.put(getKey(classInstanceName,
                          ciSingleRelationName), ciSingleRelation);
               } else if (ciItem.getValue() instanceof ClassInstanceMultiRelation) {
                  ClassInstanceMultiRelation ciMultiRelation = (ClassInstanceMultiRelation) ciItem.getValue();
                  String ciMultiRelationName = ciItem.getKey();
                  multiRelations.put(getKey(classInstanceName,
                          ciMultiRelationName), ciMultiRelation);
               } else {
                  throw new MetaModelInstanceException(
                          "Unknow Class Instance Element:"
                          + ciItem.getValue().getClass().toString());
               }
            }
         }
         setupReferences();

      }
   }

   private void setupReferences() throws Exception {
      modelInstance.updateReferences();
   }

   private String getKey(String classInstanceName, String ciLevelName) {
      return classInstanceName + "." + ciLevelName;
   }

   @Override
   public void step() throws Exception {
      logger.log(Level.INFO, "SetRows offset={0} rows={1} cols={2}!", new Object[]{getOffset(), getRows(), getCols()});
      super.step();
   }
}
